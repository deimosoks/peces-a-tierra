package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.*;
import org.icc.pecesatierra.dtos.notes.MemberNoteRequestDto;
import org.icc.pecesatierra.dtos.notes.MemberNoteResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.MemberNotes;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.MemberHasHistoricalRecordException;
import org.icc.pecesatierra.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.exceptions.MemberNoteNotFoundException;
import org.icc.pecesatierra.exceptions.ServerErrorException;
import org.icc.pecesatierra.repositories.MemberNotesRepository;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.MemberNotesMapper;
import org.icc.pecesatierra.web.services.MemberService;
import org.icc.pecesatierra.utils.PictureUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
@AllArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final MemberMapper memberMapper;
    private final AttendanceRepository attendanceRepository;
    private final MemberPersistenceService memberPersistenceService;
    private final PictureUtils pictureUtils;
    private final MemberNotesMapper memberNotesMapper;
    private final MemberNotesRepository memberNotesRepositor;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @Override
    public MemberResponseDto create(MemberRequestDto memberRequestDto) {

        Map<String, String> pictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile(), "members/photos");

        try {
            return memberPersistenceService.save(memberRequestDto, pictureData);
        } catch (Exception e) {
            if (pictureData.get("publicId") != null) {
                pictureUtils.delete(pictureData.get("publicId"));
            }
            throw new ServerErrorException("Error al procesar su solicitud, por favor intente mas tarde.");
        }
    }

    @Transactional
    @Override
    public MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        String oldPublicId = member.getPublicId();
        Map<String, String> newPictureData = null;

        if (memberRequestDto.getPictureProfile() != null) {
            newPictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile(), "members/photos");
        }

        try {
            MemberResponseDto response = memberPersistenceService.update(member, memberRequestDto, newPictureData);

            if (newPictureData != null && oldPublicId != null) {
                pictureUtils.delete(oldPublicId);
            }
            return response;

        } catch (Exception e) {
            if (newPictureData != null && newPictureData.get("publicId") != null) {
                pictureUtils.delete(newPictureData.get("publicId"));
            }
            throw new ServerErrorException("Error al procesar su solicitud, por favor intente mas tarde.");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public MemberPagesResponseDto findAll(int page, MemberFilterRequestDto dto) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by("completeName").ascending());
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Member> cq = cb.createQuery(Member.class);
        Root<Member> member = cq.from(Member.class);

        List<Predicate> predicates = buildPredicates(dto, cb, member);

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(member.get("completeName")));

        TypedQuery<Member> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Member> results = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Member> countRoot = countQuery.from(Member.class);

        List<Predicate> countPredicates = buildPredicates(dto, cb, countRoot);

        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = em.createQuery(countQuery).getSingleResult();

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        return new MemberPagesResponseDto(
                results.stream().map(m -> memberMapper.toDto(m, true)).toList(),
                totalPages
        );
    }

    @Transactional(readOnly = true)
    @Override
    public List<MemberExportDto> findAllData(MemberFilterRequestDto dto) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Member> cq = cb.createQuery(Member.class);
        Root<Member> member = cq.from(Member.class);

        List<Predicate> predicates = buildPredicates(dto, cb, member);

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.asc(member.get("completeName")));

        List<Member> results = em.createQuery(cq).getResultList();

        return results.stream()
                .map(memberMapper::toExportDto)
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void delete(String memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        if (attendanceRepository.existsByMember(member))
            throw new MemberHasHistoricalRecordException("Este integrante tiene historial de asistencias registrado asi que no puede ser eliminado del sistema, considere desactivarlo.");

        String pictureUrl = member.getPictureProfileUrl();

        memberRepository.delete(member);

        if (pictureUrl != null) pictureUtils.delete(pictureUrl);
    }

    @Transactional
    @Override
    public boolean updateActive(String memberId, boolean active) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        member.setActive(active);

        memberRepository.save(member);

        return member.isActive();
    }

    @Transactional
    @Override
    public MemberNoteResponseDto createNote(MemberNoteRequestDto memberNoteRequestDto, User user) {

        Member member = memberRepository.findById(memberNoteRequestDto.getMemberId())
                .orElseThrow(()-> new MemberNoteNotFoundException("Miembro no encontrado."));

        MemberNotes memberNotes = MemberNotes.builder()
                .note(memberNoteRequestDto.getNote())
                .createdAt(LocalDateTime.now())
                .createdBy(user.getMember())
                .member(member)
                .build();

        return memberNotesMapper.toDto(memberNotesRepositor.save(memberNotes));
    }

    @Transactional
    @Override
    public void deleteNote(String noteId) {
        MemberNotes memberNotes = memberNotesRepositor.findById(noteId)
                .orElseThrow(()-> new MemberNoteNotFoundException("Nota no encontrada."));
        memberNotesRepositor.delete(memberNotes);
    }

    private List<Predicate> buildPredicates(MemberFilterRequestDto dto, CriteriaBuilder cb, Root<Member> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (dto == null) return predicates;

        // --- Filtros Existentes ---
        if (dto.getMemberType() != null && !dto.getMemberType().isEmpty()) {
            predicates.add(root.get("typeId").get("id").in(dto.getMemberType()));
        }
        if (dto.getMemberCategory() != null && !dto.getMemberCategory().isEmpty()) {
            predicates.add(root.get("categoryId").get("id").in(dto.getMemberCategory()));
        }
        if (Boolean.TRUE.equals(dto.getOnlyActive())) {
            predicates.add(cb.equal(root.get("active"), true));
        }

        // --- Filtros Booleanos (Tiene/No Tiene) ---
        if (Boolean.TRUE.equals(dto.getHasCc())) predicates.add(cb.isNotNull(root.get("cc")));
        if (Boolean.TRUE.equals(dto.getHasCellphone())) predicates.add(cb.isNotNull(root.get("cellphone")));
        if (Boolean.TRUE.equals(dto.getHasAddress())) predicates.add(cb.isNotNull(root.get("address")));
        if (Boolean.TRUE.equals(dto.getHasBirthdate())) predicates.add(cb.isNotNull(root.get("birthdate")));

        if (Boolean.FALSE.equals(dto.getHasCc())) predicates.add(cb.isNull(root.get("cc")));
        if (Boolean.FALSE.equals(dto.getHasCellphone())) predicates.add(cb.isNull(root.get("cellphone")));
        if (Boolean.FALSE.equals(dto.getHasAddress())) predicates.add(cb.isNull(root.get("address")));
        if (Boolean.FALSE.equals(dto.getHasBirthdate())) predicates.add(cb.isNull(root.get("birthdate")));

        // --- Búsqueda General ---
        if (dto.getQuery() != null && !dto.getQuery().isBlank()) {
            String searchLike = "%" + dto.getQuery().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("completeName")), searchLike),
                    cb.like(root.get("cc"), searchLike)
            ));
        }

        // --- Filtro de Edad ---
        if (dto.getAgeFilterRange1() != null && dto.getAgeFilterRange2() != null) {
            int minAge = Math.min(dto.getAgeFilterRange1(), dto.getAgeFilterRange2());
            int maxAge = Math.max(dto.getAgeFilterRange1(), dto.getAgeFilterRange2());
            LocalDate today = LocalDate.now();
            LocalDate maxBirthdate = today.minusYears(minAge);
            LocalDate minBirthdate = today.minusYears(maxAge);
            predicates.add(cb.between(root.get("birthdate"), minBirthdate, maxBirthdate));
        }

        if (dto.getLocation() != null && !dto.getLocation().isBlank()) {
            String searchLike = "%" + dto.getLocation().toLowerCase() + "%";

            // Busca coincidencias en cualquiera de estos campos
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("address")), searchLike),
                    cb.like(cb.lower(root.get("neighborhood")), searchLike),
                    cb.like(cb.lower(root.get("city")), searchLike),
                    cb.like(cb.lower(root.get("municipality")), searchLike),
                    cb.like(cb.lower(root.get("district")), searchLike)
            ));
        }

        return predicates;
    }

}