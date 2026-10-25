package org.icc.pecesatierra.unit.member;

import org.icc.pecesatierra.features.branch.mapper.BranchMapper;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.mapper.MemberCategoryMapper;
import org.icc.pecesatierra.features.category.mapper.MemberSubCategoryMapper;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.category.repository.mapper.MemberNotesMapper;
import org.icc.pecesatierra.features.category.repository.mapper.MemberTypeMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.dtos.MemberRequestDto;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.utils.enums.Gender;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MemberMapperTest {

    @Mock
    private MemberNotesMapper memberNotesMapper;
    @Mock
    private MemberCategoryMapper memberCategoryMapper;
    @Mock
    private MemberTypeMapper memberTypeMapper;
    @Mock
    private MemberCategoryRepository memberCategoryRepository;
    @Mock
    private MemberTypeRepository memberTypeRepository;
    @Mock
    private MemberSubCategoryRepository memberSubCategoryRepository;
    @Mock
    private MemberSubCategoryMapper memberSubCategoryMapper;
    @Mock
    private BranchMapper branchMapper;
    @Mock
    private DateTimeUtils dateTimeUtils;

    private MemberMapper memberMapper() {
        return new MemberMapper(memberNotesMapper, memberCategoryMapper, memberTypeMapper,
                memberCategoryRepository, memberTypeRepository, memberSubCategoryRepository,
                memberSubCategoryMapper, branchMapper, dateTimeUtils);
    }

    private MemberRequestDto request(boolean categoryLocked) {
        return MemberRequestDto.builder()
                .completeName("Ana Perez")
                .typeId("type-1")
                .categoryId("cat-1")
                .categoryLocked(categoryLocked)
                .gender(Gender.HOMBRE)
                .build();
    }

    @Test
    void updateEntityFromDtoAppliesCategoryLockedFromRequest_H24() {
        MemberCategory category = MemberCategory.builder().id("cat-1").name("Categoria").color("#112233").build();
        MemberType type = MemberType.builder().id("type-1").name("Tipo").color("#112233").build();
        Member member = Member.builder()
                .id("member-1")
                .completeName("Ana Perez")
                .categoryLocked(false)
                .categoryId(category)
                .typeId(type)
                .build();

        memberMapper().updateEntityFromDto(request(true), member);

        // Antes el mapper hacia member.setCategoryLocked(member.isCategoryLocked()):
        // el valor del DTO se ignoraba y el bloqueo nunca cambiaba.
        assertThat(member.isCategoryLocked()).isTrue();

        memberMapper().updateEntityFromDto(request(false), member);

        assertThat(member.isCategoryLocked()).isFalse();
    }
}
