package org.icc.pecesatierra.web.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.dtos.user.*;
import org.icc.pecesatierra.entities.*;
import org.icc.pecesatierra.exceptions.members.MemberAlreadyRegisteredWithUserException;
import org.icc.pecesatierra.exceptions.members.MemberNotFoundException;
import org.icc.pecesatierra.exceptions.roles.RoleNotFoundException;
import org.icc.pecesatierra.exceptions.users.*;
import org.icc.pecesatierra.utils.mappers.UserMapper;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.RoleRepository;
import org.icc.pecesatierra.repositories.UserRepository;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.UserSpecification;
import org.icc.pecesatierra.web.services.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final UserSpecification userSpecification;

    @Transactional
    @Override
    public UserResponseDto create(UserRequestDto userRequestDto, User givenBy) {

        if (userRepository.existsByMemberId(userRequestDto.getMemberId()))
            throw new MemberAlreadyRegisteredWithUserException();

        if (userRepository.findByUsername(userRequestDto.getUsername()).isPresent())
            throw new UsernameAlreadyRegister(userRequestDto.getUsername());

        Member member = memberRepository.findById(userRequestDto.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        if (!givenBy.hasAuthority("ADMINISTRATOR") && !member.getBranch().getId().equals(givenBy.getMember().getBranch().getId())) {
            throw new CannotCreateUsersWithMembersOutsideYourBranch();
        }

        User user = User.builder()
                .username(userRequestDto.getUsername())
                .member(member)
                .passwordHash(passwordEncoder.encode(userRequestDto.getPassword()))
                .createdAt(LocalDateTime.now())
                .active(true)
                .roles(new HashSet<>())
                .build();

        userRepository.save(user);

        user.getRoles().addAll(userRequestDto.getRolesId().stream().map(roleId -> {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(RoleNotFoundException::new);

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
        }).collect(Collectors.toSet()));

        log.info("""
                        Usuario {} creó un nuevo usuario:
                        Usuario: {}
                        Integrante asociado: {} ({})
                        Roles asignados: {}
                        Creado por: {} ({})
                        """,
                user.getMember().getId(),
                user.getUsername(),
                member.getCompleteName(),
                member.getId(),
                user.getRoles().stream().map(r -> r.getRole().getName()).toList(),
                givenBy.getUsername(),
                givenBy.getId()
        );

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    @Override
    public PagesResponseDto<UserResponseDto> search(int page, String query, User user, String branchId) {

        Specification<User> spec = userSpecification.build(query, branchId, user);

        Page<User> pageResult = userRepository.findAll(
                spec,
                PageRequest.of(page, 20, Sort.by("username"))
        );

        return PagesResponseDto.<UserResponseDto>builder()
                .data(pageResult.stream().map(userMapper::toDto).toList())
                .page(pageResult.getNumber())
                .size(pageResult.getSize())
                .totalPages(pageResult.getTotalPages())
                .totalElements(pageResult.getTotalElements())
                .build();

    }

    @Transactional
    @Override
    public UserResponseDto update(UserRequestDto userRequestDto, String userId, User givenBy) {

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        Member member = memberRepository.findById(userRequestDto.getMemberId())
                .orElseThrow(MemberNotFoundException::new);

        if (givenBy.getId().equals(user.getId()))
            throw new CannotUpdateYourSelfException();

        if (!userRequestDto.getMemberId().equals(user.getMember().getId())
                && userRepository.existsByMemberId(userRequestDto.getMemberId()))
            throw new MemberAlreadyRegisteredWithUserException();

        if (!givenBy.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(givenBy.getMember().getBranch().getId())) {
            throw new CannotUpdateUserWithMemberOutSideYourBranchException();
        }

        User beforeUpdate = User.builder()
                .id(user.getId())
                .username(user.getUsername())
                .member(user.getMember())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .roles(new HashSet<>(user.getRoles()))
                .build();

        if (userRequestDto.getMemberId() != null) {
            user.setMember(member);
        }

        if (userRequestDto.getUsername() != null) {
            user.setUsername(userRequestDto.getUsername());
        }

        if (userRequestDto.getPassword() != null
                && !passwordEncoder.matches(userRequestDto.getPassword(), user.getPasswordHash())) {
            user.setPasswordHash(passwordEncoder.encode(userRequestDto.getPassword()));
        }

        Set<Role> requestedRoles = userRequestDto.getRolesId().stream().map(s -> {
            return roleRepository.findById(s)
                    .orElseThrow(RoleNotFoundException::new);
        }).collect(Collectors.toSet());

        Set<Role> currentRoles = user.getRoles().stream().map(UserRole::getRole).collect(Collectors.toSet());

        user.getRoles().removeIf(
                userRole -> !requestedRoles.contains(userRole.getRole()));

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

        userRepository.save(user);

        log.info("""
                        Usuario {} actualizó al usuario:
                        Estado anterior:
                        ID: {}
                        Username: {}
                        Integrante: {}
                        Activo: {}
                        Roles: {}
                        Estado actualizado:
                        ID: {}
                        Username: {}
                        Integrante: {}
                        Activo: {}
                        Roles: {}
                        """,
                givenBy.getId(),
                beforeUpdate.getId(),
                beforeUpdate.getUsername(),
                beforeUpdate.getMember().getCompleteName(),
                beforeUpdate.isActive(),
                beforeUpdate.getRoles().stream().map(r -> r.getRole().getName()).toList(),
                user.getId(),
                user.getUsername(),
                user.getMember().getCompleteName(),
                user.isActive(),
                user.getRoles().stream().map(r -> r.getRole().getName()).toList()
        );

        return userMapper.toDto(user);
    }

    @Transactional
    @Override
    public boolean updateActive(User user, String userId, boolean active) {

        User userToUpdate = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getId().equals(userToUpdate.getId())) {
            throw new CannotDeactivateYourselfUserException();
        }

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(userToUpdate.getMember().getBranch().getId())) {
            throw new CannotUpdateUserWithMemberOutSideYourBranchException();
        }

        boolean beforeActive = userToUpdate.isActive();

        userToUpdate.setUpdatedAt(LocalDateTime.now());

        userToUpdate.setActive(active);

        userRepository.save(userToUpdate);


        log.info("""
                        Usuario {} actualizó estado activo del usuario {}:
                        Estado anterior: {}
                        Estado actualizado: {}
                        """,
                user.getId(),
                userToUpdate.getId(),
                beforeActive,
                userToUpdate.isActive()
        );

        return userToUpdate.isActive();
    }

    @Transactional
    @Override
    public void delete(User user, String userId) {
        User userToDelete = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (!user.hasAuthority("ADMINISTRATOR") && !user.getMember().getBranch().getId().equals(userToDelete.getMember().getBranch().getId())) {
            throw new CannotUpdateUserWithMemberOutSideYourBranchException();
        }

        if (user.getId().equals(userToDelete.getId())) {
            throw new CannotDeactivateYourselfUserException();
        }

        log.info("""
                        Usuario {} eliminó al usuario:
                        ID: {}
                        Username: {}
                        Integrante: {}
                        Activo: {}
                        Roles: {}
                        """,
                user.getId(),
                userToDelete.getId(),
                user.getUsername(),
                user.getMember().getId(),
                user.isActive(),
                userToDelete.getRoles().stream()
                        .map(r -> r.getRole().getName())
                        .collect(Collectors.toSet())
        );

        userRepository.delete(userToDelete);

    }

    @Transactional(readOnly = true)
    @Override
    public UserReportResponseDto report(User user) {

        long totalUsers = user.hasAuthority("ADMINISTRATOR") ? userRepository.count() : userRepository.countByMemberBranch(user.getMember().getBranch());
        long totalUsersActives = user.hasAuthority("ADMINISTRATOR") ? userRepository.countByActiveTrue() : userRepository.countByActiveTrueAndMemberBranch(user.getMember().getBranch());

        return UserReportResponseDto.builder()
                .totalUsers(totalUsers)
                .totalUsersActives(totalUsersActives)
                .build();
    }
}
