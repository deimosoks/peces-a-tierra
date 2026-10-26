package org.icc.pecesatierra.unit.member;

import org.icc.pecesatierra.features.attendance.repository.AttendanceRepository;
import org.icc.pecesatierra.features.branch.Branch;
import org.icc.pecesatierra.configurations.ExportLimits;
import org.icc.pecesatierra.features.branch.mapper.BranchMapper;
import org.icc.pecesatierra.features.category.MemberCategory;
import org.icc.pecesatierra.features.category.MemberSubCategory;
import org.icc.pecesatierra.features.category.mapper.MemberCategoryMapper;
import org.icc.pecesatierra.features.category.mapper.MemberSubCategoryMapper;
import org.icc.pecesatierra.features.category.repository.MemberCategoryRepository;
import org.icc.pecesatierra.features.category.repository.MemberSubCategoryRepository;
import org.icc.pecesatierra.features.category.repository.mapper.MemberMapper;
import org.icc.pecesatierra.features.category.repository.mapper.MemberNotesMapper;
import org.icc.pecesatierra.features.category.repository.mapper.MemberTypeMapper;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.MemberNotes;
import org.icc.pecesatierra.features.member.dtos.MemberExportDto;
import org.icc.pecesatierra.features.member.dtos.MemberFilterRequestDto;
import org.icc.pecesatierra.features.member.dtos.MemberRequestDto;
import org.icc.pecesatierra.features.member.dtos.MemberResponseDto;
import org.icc.pecesatierra.features.member.exceptions.CannotUpdateMemberOutsideYourBranchException;
import org.icc.pecesatierra.features.member.exceptions.MemberNoHasCategoryForThisSubCategoryException;
import org.icc.pecesatierra.features.member.repository.MemberNotesRepository;
import org.icc.pecesatierra.features.member.repository.MemberRepository;
import org.icc.pecesatierra.features.member.service.MemberPersistenceService;
import org.icc.pecesatierra.features.member.service.MemberService;
import org.icc.pecesatierra.features.type.MemberType;
import org.icc.pecesatierra.features.type.repository.MemberTypeRepository;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.cloudinary.PictureUtils;
import org.icc.pecesatierra.utils.exceptions.ExportLimitExceededException;
import org.icc.pecesatierra.utils.models.ExportResponseDto;
import org.icc.pecesatierra.utils.models.PagesResponseDto;
import org.icc.pecesatierra.utils.specs.MemberSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberMapper memberMapper;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private MemberPersistenceService memberPersistenceService;
    @Mock
    private PictureUtils pictureUtils;
    @Mock
    private MemberSpecification memberSpecification;
    @Mock
    private MemberNotesRepository memberNotesRepository;

    @InjectMocks
    private MemberService memberService;

    private final Branch branchA = TestFixtures.branch("branch-a", "Sede A");

    @Test
    void deleteSendsCloudinaryPublicIdInsteadOfUrl_H31() {
        Member member = TestFixtures.member("member-1", branchA, "Ana Perez");
        member.setPictureProfileUrl("https://res.cloudinary.com/demo/image/upload/v1/members/photos/abc123.jpg");
        member.setPublicId("members/photos/abc123");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(attendanceRepository.existsByMember(member)).thenReturn(false);

        memberService.delete(member.getId(), user);

        verify(memberRepository).delete(member);
        verify(pictureUtils).delete("members/photos/abc123");
        verify(pictureUtils, never())
                .delete("https://res.cloudinary.com/demo/image/upload/v1/members/photos/abc123.jpg");
    }

    @Test
    void deleteOnMemberFromAnotherBranchIsRejectedBeforeAttendanceLookup_M03() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member member = TestFixtures.member("member-1", branchB, "Ana Perez");
        member.setPublicId("members/photos/abc123");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.delete(member.getId(), user))
                .isInstanceOfSatisfying(CannotUpdateMemberOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        // La sede corta antes de revelar si el integrante ajeno tiene asistencias.
        verify(attendanceRepository, never()).existsByMember(any(Member.class));
        verify(memberRepository, never()).delete(any(Member.class));
        verifyNoInteractions(pictureUtils);
    }

    @Test
    void updateOfForeignMemberChecksBranchBeforeCcUniquenessAndPhoto_M03() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member member = TestFixtures.member("member-1", branchB, "Ana Perez B");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        MemberRequestDto request = MemberRequestDto.builder()
                .cc("999")
                .pictureProfile(mock(MultipartFile.class))
                .build();

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.update(request, member.getId(), user))
                .isInstanceOfSatisfying(CannotUpdateMemberOutsideYourBranchException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).doesNotContain("Ana Perez B");
                });

        // Ni se consulta la CC ajena ni se sube la foto antes de rechazar la sede.
        verify(memberRepository, never()).existsByCc(any());
        verifyNoInteractions(pictureUtils, memberPersistenceService);
    }

    @Test
    void updateActiveOnMemberFromAnotherBranchIsRejected_M03() {
        Branch branchB = TestFixtures.branch("branch-b", "Sede B");
        Member member = TestFixtures.member("member-1", branchB, "Ana Perez");
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberService.updateActive(member.getId(), false, user))
                .isInstanceOfSatisfying(CannotUpdateMemberOutsideYourBranchException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(member.isActive()).isTrue();
        verify(memberRepository, never()).save(any(Member.class));
    }

    @Test
    void createWithoutPicturePropagatesTheBusinessException_H23() {
        User user = TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
        MemberRequestDto request = MemberRequestDto.builder()
                .completeName("Ana Perez")
                .categoryId("categoria-inexistente")
                .build();
        MemberNoHasCategoryForThisSubCategoryException businessException =
                new MemberNoHasCategoryForThisSubCategoryException("Ana Perez");

        when(memberPersistenceService.save(any(MemberRequestDto.class), isNull(), any(User.class)))
                .thenThrow(businessException);

        // El alta sin foto deja pictureData en null: el catch no debe desreferenciarlo.
        assertThatThrownBy(() -> memberService.create(request, user))
                .isSameAs(businessException)
                .isNotInstanceOf(NullPointerException.class);

        verifyNoInteractions(pictureUtils);
    }

    @Test
    void deleteWithoutPublicIdDoesNotCallCloudinary() {
        Member member = TestFixtures.member("member-1", branchA, "Ana Perez");
        member.setPictureProfileUrl("https://res.cloudinary.com/demo/image/upload/v1/legacy.jpg");
        member.setPublicId(null);
        User admin = TestFixtures.user("admin-1", "admin-a",
                TestFixtures.member("admin-member-1", branchA, "Admin A"), "ADMINISTRATOR");

        when(memberRepository.findById(member.getId())).thenReturn(Optional.of(member));
        when(attendanceRepository.existsByMember(member)).thenReturn(false);

        memberService.delete(member.getId(), admin);

        verify(memberRepository).delete(member);
        verifyNoInteractions(pictureUtils);
    }

    // ---------- H-32: techo de la exportacion ----------

    private MemberService serviceWithExportLimit(int maxRows) {
        return new MemberService(memberRepository, memberMapper, attendanceRepository,
                memberPersistenceService, pictureUtils, memberSpecification, new ExportLimits(maxRows),
                memberNotesRepository);
    }

    private User operator() {
        return TestFixtures.user("user-1", "operador-a",
                TestFixtures.member("operator-1", branchA, "Operador A"));
    }

    @Test
    void exportUnderTheLimitReturnsEveryRow_H32() {
        User user = operator();
        Member first = TestFixtures.member("member-1", branchA, "Ana Perez");
        Member second = TestFixtures.member("member-2", branchA, "Beto Perez");
        Specification<Member> spec = mock(Specification.class);

        when(memberSpecification.build(any(MemberFilterRequestDto.class), any(User.class))).thenReturn(spec);
        when(memberRepository.count(spec)).thenReturn(2L);
        when(memberRepository.findAll(spec)).thenReturn(List.of(first, second));
        when(memberMapper.toExportDto(any(Member.class)))
                .thenAnswer(invocation -> MemberExportDto.builder()
                        .completeName(invocation.getArgument(0, Member.class).getCompleteName())
                        .build());

        ExportResponseDto<MemberExportDto> response =
                serviceWithExportLimit(2).export(new MemberFilterRequestDto(), user);

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getData())
                .extracting(MemberExportDto::getCompleteName)
                .containsExactly("Ana Perez", "Beto Perez");
    }

    @Test
    void exportAboveTheLimitIsRejectedWithoutFetchingTheRows_H32() {
        User user = operator();
        Specification<Member> spec = mock(Specification.class);

        when(memberSpecification.build(any(MemberFilterRequestDto.class), any(User.class))).thenReturn(spec);
        when(memberRepository.count(spec)).thenReturn(3L);

        assertThatThrownBy(() -> serviceWithExportLimit(2).export(new MemberFilterRequestDto(), user))
                .isInstanceOfSatisfying(ExportLimitExceededException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getMessage()).contains("rango de fechas");
                });

        // Solo se cuenta: no se materializa ninguna fila ni se mapea nada.
        verify(memberRepository, never()).findAll(any(Specification.class));
        verifyNoInteractions(memberMapper);
    }

    // ---------- H-39: notas de la busqueda sin N+1 ----------

    private MemberNotes note(String id, Member member) {
        return MemberNotes.builder()
                .id(id)
                .note("Nota " + id)
                .member(member)
                .createdBy(member)
                .createdAt(TestFixtures.FIXED_NOW)
                .build();
    }

    @Test
    void searchWithoutNotesDoesNotReadTheNotesCollection_H39() {
        Member member = TestFixtures.member("member-1", branchA, "Ana Perez");
        Specification<Member> spec = mock(Specification.class);

        when(memberSpecification.build(any(MemberFilterRequestDto.class), any(User.class))).thenReturn(spec);
        when(memberRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(member)));
        when(memberMapper.toDto(member, false))
                .thenReturn(MemberResponseDto.builder().id("member-1").build());

        PagesResponseDto<MemberResponseDto> response = memberService.search(0, null, operator(), false);

        assertThat(response.getData()).extracting(MemberResponseDto::getId).containsExactly("member-1");
        // El caso sin notas no lanza ninguna consulta de notas ni toca la coleccion.
        verifyNoInteractions(memberNotesRepository);
        verify(memberMapper, never()).toDto(any(Member.class), any(Collection.class));
    }

    @Test
    void searchWithNotesLoadsEveryNoteOfThePageInASingleQuery_H39() {
        Member first = TestFixtures.member("member-1", branchA, "Ana Perez");
        Member second = TestFixtures.member("member-2", branchA, "Beto Perez");
        MemberNotes firstNote = note("note-1", first);
        MemberNotes secondNote = note("note-2", second);
        Specification<Member> spec = mock(Specification.class);

        when(memberSpecification.build(any(MemberFilterRequestDto.class), any(User.class))).thenReturn(spec);
        when(memberRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 20), 2));
        when(memberNotesRepository.findByMember_IdIn(List.of(first.getId(), second.getId())))
                .thenReturn(List.of(firstNote, secondNote));
        when(memberMapper.toDto(any(Member.class), any(Collection.class)))
                .thenReturn(MemberResponseDto.builder().id("member").build());

        memberService.search(0, null, operator(), true);

        // Una sola consulta para las notas de toda la pagina...
        verify(memberNotesRepository, times(1)).findByMember_IdIn(List.of(first.getId(), second.getId()));

        // ...y cada integrante recibe solo las suyas.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<MemberNotes>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(memberMapper).toDto(eq(first), captor.capture());
        assertThat(captor.getValue()).containsExactly(firstNote);
        verify(memberMapper).toDto(eq(second), captor.capture());
        assertThat(captor.getAllValues().get(1)).containsExactly(secondNote);
    }

    @Test
    void pagedSearchDoesNotFetchTheNotesCollection_H39() throws NoSuchMethodException {
        Method pageQuery = MemberRepository.class.getMethod("findAll", Specification.class, Pageable.class);
        EntityGraph pageGraph = pageQuery.getAnnotation(EntityGraph.class);

        // La consulta paginada no puede unir la coleccion: Hibernate paginaria en
        // memoria. Las notas van por el segundo camino (lote por ids).
        assertThat(pageGraph).isNotNull();
        assertThat(pageGraph.attributePaths()).doesNotContain("notes");

        Method notesQuery = MemberNotesRepository.class.getMethod("findByMember_IdIn", Collection.class);
        EntityGraph notesGraph = notesQuery.getAnnotation(EntityGraph.class);
        assertThat(notesGraph).isNotNull();
        assertThat(notesGraph.attributePaths()).contains("createdBy");
    }

    // ---------- H-39: grafo de la exportacion ----------

    @Test
    void exportEntityGraphFetchesOnlyTheToOneAssociationsReadByTheExportMapper_H39() throws NoSuchMethodException {
        Method exportQuery = MemberRepository.class.getMethod("findAll", Specification.class);
        EntityGraph graph = exportQuery.getAnnotation(EntityGraph.class);

        assertThat(graph).isNotNull();
        // toExportDto solo pide el nombre del tipo, de la categoria y de la
        // subcategoria, asi que esas tres to-one bastan.
        assertThat(graph.attributePaths()).containsExactlyInAnyOrder("typeId", "categoryId", "subcategoryId");
        // notes es una coleccion: unirla multiplicaria las filas del export.
        // branch/registeredBy no los lee el mapper de exportacion.
        assertThat(graph.attributePaths()).doesNotContain("notes", "branch", "registeredBy");
    }

    @Test
    void exportMapsTheSameFieldsForEveryRowAfterPreloadingTheGraph_H39() {
        DateTimeUtils dateTimeUtils = mock(DateTimeUtils.class);
        when(dateTimeUtils.nowColombia()).thenReturn(LocalDateTime.of(2026, 1, 15, 7, 0));
        MemberMapper realMapper = new MemberMapper(
                mock(MemberNotesMapper.class),
                mock(MemberCategoryMapper.class),
                mock(MemberTypeMapper.class),
                mock(MemberCategoryRepository.class),
                mock(MemberTypeRepository.class),
                mock(MemberSubCategoryRepository.class),
                mock(MemberSubCategoryMapper.class),
                mock(BranchMapper.class),
                dateTimeUtils);

        MemberType type = MemberType.builder().id("type-1").name("Adulto").color("#112233").build();
        MemberCategory category = MemberCategory.builder().id("cat-1").name("Jovenes").color("#112233").build();
        MemberSubCategory subCategory = MemberSubCategory.builder()
                .id("sub-1").name("Universitarios").color("#112233").category(category).build();
        Member member = TestFixtures.member("member-1", branchA, "Ana Perez");
        member.setTypeId(type);
        member.setCategoryId(category);
        member.setSubcategoryId(subCategory);
        member.setGender("F");
        member.setCellphone("3000000000");
        member.setAddress("Calle 1 # 2-3");
        member.setCc("123456789");
        member.setBirthdate(LocalDate.of(2000, 1, 1));

        MemberService service = new MemberService(memberRepository, realMapper, attendanceRepository,
                memberPersistenceService, pictureUtils, memberSpecification, new ExportLimits(10),
                memberNotesRepository);
        Specification<Member> spec = mock(Specification.class);
        when(memberSpecification.build(any(MemberFilterRequestDto.class), any(User.class))).thenReturn(spec);
        when(memberRepository.count(spec)).thenReturn(1L);
        when(memberRepository.findAll(spec)).thenReturn(List.of(member));

        ExportResponseDto<MemberExportDto> response = service.export(new MemberFilterRequestDto(), operator());

        // Las mismas columnas que el export entregaba antes del grafo.
        assertThat(response.getTotalElements()).isEqualTo(1);
        MemberExportDto dto = response.getData().get(0);
        assertThat(dto.getCompleteName()).isEqualTo("Ana Perez");
        assertThat(dto.getType()).isEqualTo("Adulto");
        assertThat(dto.getCategory()).isEqualTo("Jovenes");
        assertThat(dto.getSubCategory()).isEqualTo("Universitarios");
        assertThat(dto.getGender()).isEqualTo("F");
        assertThat(dto.getCellphone()).isEqualTo("3000000000");
        assertThat(dto.getAddress()).isEqualTo("Calle 1 # 2-3");
        assertThat(dto.getCc()).isEqualTo("123456789");
        assertThat(dto.getBirthdate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(dto.getAge()).isEqualTo(26);
    }
}
