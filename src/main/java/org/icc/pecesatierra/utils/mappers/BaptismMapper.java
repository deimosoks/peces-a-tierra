package org.icc.pecesatierra.utils.mappers;

import org.icc.pecesatierra.dtos.baptism.BaptismResponseDto;
import org.icc.pecesatierra.entities.Baptism;
import org.springframework.stereotype.Component;

@Component
public class BaptismMapper {

    public BaptismResponseDto toDto(Baptism baptism) {
        return BaptismResponseDto.builder()
                .id(baptism.getId())
                .memberName(baptism.getBaptizedMember().getCompleteName())
                .date(baptism.getDate())
                .note(baptism.getNote())
                .address(baptism.getAddress())
                .createdAt(baptism.getCreatedAt())
                .branchName(baptism.getBaptizedMember().getBranch().getName())
                .registeredBy(baptism.getRegisteredBy().getCompleteName())
                .invalidReason(baptism.getInvalidReason())
                .invalidAt(baptism.getInvalidAt())
                .invalidatedBy(baptism.getInvalidatorId() != null ? baptism.getInvalidatorId().getCompleteName() : null)
                .invalid(baptism.isInvalid())
                .build();
    }

}
