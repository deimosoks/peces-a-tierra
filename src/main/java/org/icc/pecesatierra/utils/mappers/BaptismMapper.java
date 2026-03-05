package org.icc.pecesatierra.utils.mappers;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.baptism.BaptismResponseDto;
import org.icc.pecesatierra.entities.Baptism;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BaptismMapper {

    private final DateTimeUtils dateTimeUtils;

    public BaptismResponseDto toDto(Baptism baptism) {
        return BaptismResponseDto.builder()
                .id(baptism.getId())
                .memberName(baptism.getBaptizedMember().getCompleteName())
                .date(baptism.getDate())
                .note(baptism.getNote())
                .address(baptism.getAddress())
                .createdAt(dateTimeUtils.toColombia(baptism.getCreatedAt()))
                .branchName(baptism.getBaptizedMember().getBranch().getName())
                .registeredBy(baptism.getRegisteredBy().getCompleteName())
                .invalidReason(baptism.getInvalidReason())
                .invalidAt(dateTimeUtils.toColombia(baptism.getInvalidAt()))
                .invalidatedBy(baptism.getInvalidatorId() != null ? baptism.getInvalidatorId().getCompleteName() : null)
                .invalid(baptism.isInvalid())
                .build();
    }

}
