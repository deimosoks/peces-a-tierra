package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.domain.entities.*;
import org.icc.pecesatierra.dtos.user.*;
import org.icc.pecesatierra.exceptions.*;
import org.icc.pecesatierra.utils.mappers.UserMapper;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.RoleRepository;
import org.icc.pecesatierra.repositories.UserRepository;
import org.icc.pecesatierra.web.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    @Transactional
    @Override
    public UserResponseDto create(UserRequestDto userRequestDto, User givenBy) {

        if (userRepository.existsByMemberId(userRequestDto.getMemberId()))
            throw new MemberAlreadyRegisteredWithUserException("Member is already related to a user.");

        if (userRepository.findByUsername(userRequestDto.getUsername()).isPresent())
            throw new UsernameAlreadyRegister("Username already register.");

        Member member = memberRepository.findById(userRequestDto.getMemberId())
                .orElseThrow(() -> new MemberNotFoundException("Member doesn't exist."));

        User user = User.builder()
                .username(userRequestDto.getUsername())
                .member(member)
                .passwordHash(passwordEncoder.encode(userRequestDto.getPassword()))
                .createdAt(LocalDateTime.now())
                .active(true)
                .roles(new ArrayList<>())
                .build();

        userRepository.save(user);

        user.getRoles().addAll(userRequestDto.getRolesId().stream().map(roleId ->
                {
                    Role role = roleRepository.findById(roleId)
                            .orElseThrow(() -> new RoleNotFoundException("Role doesn't exist."));

                    return UserRole.builder()
                            .id(UserRoleId.builder()
                                    .roleId(role.getId())
                                    .userId(user.getId())
                                    .build())
                            .user(user)
                            .role(role)
                            .giverId(givenBy.getMember().getId())
                            .givenDate(LocalDateTime.now())
                            .build();
                }
        ).collect(Collectors.toSet()));

        return userMapper.toDto(user);
    }

    @Override
    public MeDto me(User user) {
        return MeDto.builder()
                .username(user.getUsername())
                .pictureProfileUrl(user.getMember().getPictureProfileUrl())
                .completeName(user.getMember().getCompleteName())
                .permissions(
                        user.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public UserPagesResponseDto findAll(int page, String query) {

        Pageable pageable = PageRequest.of(page, 20, Sort.by("id").ascending());

        Page<User> users = userRepository.findAll(query, pageable);

        int totalPages = users.getTotalPages();

        List<UserResponseDto> usersDto = users.stream().map(userMapper::toDto).toList();

        return UserPagesResponseDto.builder()
                .users(usersDto)
                .pages(totalPages)
                .build();
    }

    @Transactional
    @Override
    public UserResponseDto update(UserRequestDto userRequestDto, String userId, User givenBy) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User doesn't exist."));
        Member member = memberRepository.findById(userRequestDto.getMemberId())
                .orElseThrow(() -> new MemberNotFoundException("Member doesn't exist."));

        if (!userRequestDto.getMemberId().equals(user.getMember().getId()) && userRepository.existsByMemberId(userRequestDto.getMemberId()))
            throw new MemberAlreadyRegisteredWithUserException("Member is already related to a user.");

        if (userRequestDto.getMemberId() != null) {
            user.setMember(member);
        }

        if (userRequestDto.getUsername() != null) {
            user.setUsername(userRequestDto.getUsername());
        }

        if (userRequestDto.getPassword() != null && !passwordEncoder.matches(userRequestDto.getPassword(), user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(userRequestDto.getPassword()));
        }

        Set<Role> requestedRoles = userRequestDto.getRolesId().stream().map(s -> {
            return roleRepository.findById(s)
                    .orElseThrow(() -> new RoleNotFoundException("Role doesn't exist."));
        }).collect(Collectors.toSet());

        Set<Role> currentRoles = user.getRoles().stream().map(UserRole::getRole).collect(Collectors.toSet());

        user.getRoles().removeIf(
                userRole -> !requestedRoles.contains(userRole.getRole())
        );

        requestedRoles.forEach(role -> {
            if (!currentRoles.contains(role)) {
                UserRole userRole = UserRole.builder()
                        .id(UserRoleId.builder()
                                .userId(user.getId())
                                .roleId(role.getId())
                                .build())
                        .user(user)
                        .role(role)
                        .giverId(givenBy.getMember().getId())
                        .givenDate(LocalDateTime.now())
                        .build();

                user.getRoles().add(userRole);
            }
        });

        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toDto(userRepository.save(user));
    }

    @Override
    public boolean updateActive(String userId, boolean active) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User doesn't exist."));

        user.setUpdatedAt(LocalDateTime.now());

        user.setActive(active);

        userRepository.save(user);

        return user.isActive();
    }

    @Override
    public void delete(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User doesn't exist"));

        userRepository.delete(user);

    }

    @Override
    public UserReportResponseDto report() {

        long totalUsers = userRepository.count();
        long totalUsersActives = userRepository.countByActiveTrue();

        return UserReportResponseDto.builder()
                .totalUsers(totalUsers)
                .totalUsersActives(totalUsersActives)
                .build();
    }
}
