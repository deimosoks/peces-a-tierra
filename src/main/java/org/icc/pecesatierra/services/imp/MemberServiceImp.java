package org.icc.pecesatierra.services.imp;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.exceptions.MemberHasHistoricalRecordException;
import org.icc.pecesatierra.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.services.MemberService;
import org.icc.pecesatierra.utils.PictureManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@AllArgsConstructor
public class MemberServiceImp implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final AttendanceRepository attendanceRepository;
    private final PictureManager pictureManager;

    @Transactional
    @Override
    public MemberResponseDto create(MemberRequestDto memberRequestDto) {

        Member member = Member.builder()
                .type(memberRequestDto.getType().toString())
                .category(memberRequestDto.getCategory().toString())
                .completeName(memberRequestDto.getCompleteName())
                .createdAt(LocalDateTime.now())
                .cc(memberRequestDto.getCc())
                .cellphone(memberRequestDto.getCellphone())
                .address(memberRequestDto.getAddress())
                .birthdate(memberRequestDto.getBirthdate())
                .active(true)
//                    .pictureProfileUrl("/uploads/" + newFileName)
                .build();

        String newFileName = pictureManager.validatePicture(memberRequestDto.getPictureProfile());

        if (!newFileName.isBlank())
            member.setPictureProfileUrl("/uploads/" + newFileName);

        return memberMapper.toDto(memberRepository.save(member));

    }

    @Transactional
    @Override
    public MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member doesn't exist"));

        memberMapper.updateEntityFromDto(memberRequestDto, member);

        member.setUpdatedAt(LocalDateTime.now());

//        memberRepository.save(member);

        if (memberRequestDto.getPictureProfile() != null &&
                member.getPictureProfileUrl() != null &&
                !member.getPictureProfileUrl().equals(memberRequestDto.getPictureProfile().getOriginalFilename())) {
            pictureManager.delete(member.getPictureProfileUrl());

            String newFileName = pictureManager.validatePicture(memberRequestDto.getPictureProfile());

            if (!newFileName.isBlank()) {
                member.setPictureProfileUrl("/uploads/" + newFileName);
            }
        } else if (member.getPictureProfileUrl() == null && memberRequestDto.getPictureProfile() != null){
            String newFileName = pictureManager.validatePicture(memberRequestDto.getPictureProfile());

            if (!newFileName.isBlank()) {
                member.setPictureProfileUrl("/uploads/" + newFileName);
            }
        }

            return memberMapper.toDto(memberRepository.save(member));
    }

    @Override
    public MemberPagesResponseDto findAll(int page, boolean onlyActives) {

        Pageable pageable = PageRequest.of(page, 20, Sort.by("id").ascending());

        Page<Member> members = onlyActives ? memberRepository.findAllByActiveTrue(pageable) : memberRepository.findAll(pageable);

        int totalPages = members.getTotalPages();

        return new MemberPagesResponseDto(members.stream().map(memberMapper::toDto).toList(), totalPages);
    }

    @Override
    public MemberPagesResponseDto findByQuery(String query, int page, boolean onlyActive) {

        Pageable pageable = PageRequest.of(page, 20);

        Page<Member> members = onlyActive ? memberRepository.findByQueryActive(query, pageable) : memberRepository.findByQuery(query, pageable);

        int totalPages = members.getTotalPages();

        List<MemberResponseDto> memberResponseDto = members.stream().map(memberMapper::toDto).toList();

        return new MemberPagesResponseDto(memberResponseDto, totalPages);
    }

    @Transactional
    @Override
    public void delete(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member doesn't exist."));

        if (attendanceRepository.existsByMember(member))
            throw new MemberHasHistoricalRecordException("Member Member has historical record and cannot be deleted");

        String pictureUrl = member.getPictureProfileUrl();

        memberRepository.delete(member);

        if (pictureUrl != null)pictureManager.delete(pictureUrl);
    }

    @Override
    public boolean updateActive(String memberId, boolean active) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Member doesn't exist."));

        member.setActive(active);

        memberRepository.save(member);

        return member.isActive();
    }


}
