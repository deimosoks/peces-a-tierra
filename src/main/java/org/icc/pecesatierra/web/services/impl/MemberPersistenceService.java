package org.icc.pecesatierra.web.services.impl;

import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@AllArgsConstructor
@Service
public class MemberPersistenceService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;

    @Transactional
    public MemberResponseDto save(MemberRequestDto dto, Map<String, String> pictureData) {
        Member member = Member.builder()
                .type(dto.getType().toString())
                .category(dto.getCategory().toString())
                .completeName(dto.getCompleteName())
                .createdAt(LocalDateTime.now())
                .cc(dto.getCc())
                .cellphone(dto.getCellphone())
//                .address(dto.getAddress())
                .birthdate(dto.getBirthdate())
                .active(true)
                .build();
        member.setNeighborhood(dto.getNeighborhood());
        member.setCity(dto.getCity());
        member.setMunicipality(dto.getMunicipality());
        member.setDistrict(dto.getDistrict());
        member.setPostalCode(dto.getPostalCode());
        member.setLatitude(dto.getLatitude());
        member.setLongitude(dto.getLongitude());

        if (pictureData.get("url") != null) {
            member.setPictureProfileUrl(pictureData.get("url"));
            member.setPublicId(pictureData.get("publicId"));
        }
        return memberMapper.toDto(memberRepository.save(member), false);
    }

    @Transactional
    public MemberResponseDto update(Member member, MemberRequestDto dto, Map<String, String> picData) {
        memberMapper.updateEntityFromDto(dto, member);
        member.setUpdatedAt(LocalDateTime.now());

        if (picData != null && picData.get("url") != null) {
            member.setPictureProfileUrl(picData.get("url"));
            member.setPublicId(picData.get("publicId"));
        }
        return memberMapper.toDto(memberRepository.save(member), true);
    }

}
