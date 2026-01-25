package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.member.MemberFilterRequestDto;
import org.icc.pecesatierra.dtos.member.MemberPagesResponseDto;
import org.icc.pecesatierra.dtos.member.MemberRequestDto;
import org.icc.pecesatierra.dtos.member.MemberResponseDto;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.exceptions.MemberHasHistoricalRecordException;
import org.icc.pecesatierra.exceptions.MemberNotFoundException;
import org.icc.pecesatierra.exceptions.ServerErrorException;
import org.icc.pecesatierra.utils.mappers.MemberMapper;
import org.icc.pecesatierra.repositories.AttendanceRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.web.services.MemberService;
import org.icc.pecesatierra.utils.PictureUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @PersistenceContext
    private EntityManager em;

    @Override
    public MemberResponseDto create(MemberRequestDto memberRequestDto) {

        Map<String, String> pictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile());

        try {
            return memberPersistenceService.save(memberRequestDto, pictureData);
        } catch (Exception e) {
            if (pictureData.get("publicId") != null) {
                pictureUtils.delete(pictureData.get("publicId"));
            }
            throw new ServerErrorException("Error al procesar su solicitud, por favor intente mas tarde.");
        }
    }

    @Override
    public MemberResponseDto update(MemberRequestDto memberRequestDto, String memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        String oldPublicId = member.getPublicId();
        Map<String, String> newPictureData = null;

        if (memberRequestDto.getPictureProfile() != null) {
            newPictureData = pictureUtils.validateAndSavePicture(memberRequestDto.getPictureProfile());
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

    @Override
    public MemberPagesResponseDto findAll(int page, MemberFilterRequestDto dto) {

        Pageable pageable = PageRequest.of(page, 20, Sort.by("createdAt").descending());

        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Member> cq = cb.createQuery(Member.class);
        Root<Member> member = cq.from(Member.class);

        List<Predicate> predicates = new ArrayList<>();

        if (dto != null) {
            if (dto.getMemberType() != null && !dto.getMemberType().isEmpty()) {
                predicates.add(member.get("type").in(dto.getMemberType()));
            }

            if (dto.getMemberCategory() != null && !dto.getMemberCategory().isEmpty()) {
                predicates.add(member.get("category").in(dto.getMemberCategory()));
            }

            if (dto.isOnlyActive()) {
                predicates.add(cb.equal(member.get("active"), true));
            }

            if (dto.getQuery() != null && !dto.getQuery().isBlank()) {
                String searchLike = "%" + dto.getQuery().toLowerCase() + "%";

                Predicate nameLike = cb.like(cb.lower(member.get("completeName")), searchLike);
                Predicate ccLike = cb.like(member.get("cc"), searchLike); // CC suele ser numérico/exacto, pero like sirve para parciales

                predicates.add(cb.or(nameLike, ccLike));
            }
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(member.get("createdAt")));

        TypedQuery<Member> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Member> results = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Member> countRoot = countQuery.from(Member.class);

        List<Predicate> countPredicates = new ArrayList<>();

        if (dto != null) {
            if (dto.getMemberType() != null && !dto.getMemberType().isEmpty()) {
                countPredicates.add(countRoot.get("type").in(dto.getMemberType()));
            }

            if (dto.getMemberCategory() != null && !dto.getMemberCategory().isEmpty()) {
                countPredicates.add(countRoot.get("category").in(dto.getMemberCategory()));
            }

            if (dto.isOnlyActive()) {
                countPredicates.add(cb.equal(countRoot.get("active"), true));
            }

            if (dto.getQuery() != null && !dto.getQuery().isBlank()) {
                String searchLike = "%" + dto.getQuery().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(countRoot.get("completeName")), searchLike);
                Predicate ccLike = cb.like(countRoot.get("cc"), searchLike);

                countPredicates.add(cb.or(nameLike, ccLike));
            }
        }

        countQuery.select(cb.count(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = em.createQuery(countQuery).getSingleResult();

        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        return new MemberPagesResponseDto(
                results.stream().map(memberMapper::toDto).toList(),
                totalPages
        );
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

    @Override
    public boolean updateActive(String memberId, boolean active) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberNotFoundException("Este integrante no existe."));

        member.setActive(active);

        memberRepository.save(member);

        return member.isActive();
    }


}