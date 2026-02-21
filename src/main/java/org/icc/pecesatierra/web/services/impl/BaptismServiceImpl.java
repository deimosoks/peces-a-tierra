package org.icc.pecesatierra.web.services.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.dtos.baptism.*;
import org.icc.pecesatierra.entities.Baptism;
import org.icc.pecesatierra.entities.Member;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.exceptions.*;
import org.icc.pecesatierra.repositories.BaptismRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.utils.mappers.BaptismMapper;
import org.icc.pecesatierra.web.services.BaptismService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class BaptismServiceImpl implements BaptismService {

    private final BaptismRepository baptismRepository;
    private final MemberRepository memberRepository;
    private final BaptismMapper baptismMapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @Override
    public BaptismResponseDto create(BaptismRequestDto baptismRequestDto, User user) {

        Member member = memberRepository.findById(baptismRequestDto.getMemberId())
                .orElseThrow(() -> new MemberNoteNotFoundException("Miembro no encontrado."));

        if (baptismRepository.existsByBaptizedMemberAndInvalidFalse(member)) {
            throw new MemberAlreadyHasRegisteredBaptismActiveException("Este integrante ya tiene un bautismo valido registrado.");
        }

        if (!user.hasAuthority("ADMINISTRATOR") && !member.getBranch().equals(user.getMember().getBranch())) {
            throw new CannotRegisterBaptismOutsideYourBranch("No puedes registrar bautismos a integrantes fuera de tu sede.");
        }

        Baptism baptism = Baptism.builder()
                .baptizedMember(member)
                .date(baptismRequestDto.getDate())
                .neighborhood(baptismRequestDto.getNote())
                .createdAt(LocalDateTime.now())
                .registeredBy(user.getMember())
                .address(baptismRequestDto.getAddress())
                .neighborhood(baptismRequestDto.getNeighborhood())
                .city(baptismRequestDto.getCity())
                .municipality(baptismRequestDto.getCity())
                .district(baptismRequestDto.getDistrict())
                .postalCode(baptismRequestDto.getPostalCode())
                .latitude(baptismRequestDto.getLatitude())
                .longitude(baptismRequestDto.getLongitude())
                .invalid(false)
                .build();

        return baptismMapper.toDto(baptismRepository.save(baptism));
    }

    @Transactional
    @Override
    public BaptismResponseDto invalid(BaptismInvalidRequestDto baptismInvalidRequestDto, User user) {

        Baptism baptism = baptismRepository.findById(baptismInvalidRequestDto.getBaptismId())
                .orElseThrow(() -> new BaptismNotFoundException("Bautismo no encontrado."));

        if (!user.hasAuthority("ADMINISTRATOR") && !baptism.getBaptizedMember().getBranch().equals(user.getMember().getBranch())) {
            throw new CannotCreateMembersOutsideYourBranch("No puedes registrar bautismos a integrantes fuera de tu sede.");
        }

        baptism.setInvalid(true);
        baptism.setInvalidatorId(user.getMember());
        baptism.setInvalidReason(baptismInvalidRequestDto.getInvalidReason());
        baptism.setInvalidAt(LocalDateTime.now());

        return baptismMapper.toDto(baptismRepository.save(baptism));
    }

    @Transactional(readOnly = true)
    @Override
    public BaptismPagesResponseDto findAll(int page, BaptismFilterRequestDto dto, User user) {

        Pageable pageable = PageRequest.of(page, 20, Sort.by("date").descending());

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Baptism> cq = cb.createQuery(Baptism.class);
        Root<Baptism> baptism = cq.from(Baptism.class);

        // fetch joins
        Fetch<Baptism, Member> baptizedFetch = baptism.fetch("baptizedMember", JoinType.INNER);
        baptism.fetch("registeredBy", JoinType.INNER);

        Join<Baptism, Member> baptizedMember = (Join<Baptism, Member>) baptizedFetch;

        List<Predicate> predicates = new ArrayList<>();

        if (dto != null) {

            if (dto.getBranchId() != null) {
                if (user.hasAuthority("ADMINISTRATOR")) {
                    predicates.add(cb.equal(baptizedMember.get("branch").get("id"), dto.getBranchId()));
                } else {
                    predicates.add(cb.equal(baptizedMember.get("branch"), user.getMember().getBranch()));
                }
            } else if (!user.hasAuthority("ADMINISTRATOR")) {
                predicates.add(cb.equal(baptizedMember.get("branch"), user.getMember().getBranch()));
            }

            // memberId
            if (dto.getMemberId() != null) {
                predicates.add(cb.equal(baptizedMember.get("id"), dto.getMemberId()));
            }

            // ===== FILTRO POR FECHA DE BAUTISMO =====

            if (dto.getStartDate() != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                baptism.get("date"),
                                dto.getStartDate()
                        )
                );
            }

            if (dto.getEndDate() != null) {
                predicates.add(
                        cb.lessThanOrEqualTo(
                                baptism.get("date"),
                                dto.getEndDate()
                        )
                );
            }

            // active → solo no inválidos
            if (dto.isActive()) {
                predicates.add(cb.isFalse(baptism.get("invalid")));
            }

            // ===== LOCATION GLOBAL =====

            if (dto.getQuery() != null && !dto.getQuery().isBlank()) {

                String like = "%" + dto.getQuery().toLowerCase() + "%";

                Predicate queryPredicate = cb.or(
                        cb.like(cb.lower(baptizedMember.get("completeName")), like),

                        cb.like(cb.lower(baptism.get("neighborhood")), like),
                        cb.like(cb.lower(baptism.get("city")), like),
                        cb.like(cb.lower(baptism.get("municipality")), like),
                        cb.like(cb.lower(baptism.get("district")), like),
                        cb.like(cb.lower(baptism.get("postalCode")), like)
                );

                predicates.add(queryPredicate);
            }
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(baptism.get("date")));

        TypedQuery<Baptism> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Baptism> results = query.getResultList();

        // ===== COUNT QUERY =====

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Baptism> countRoot = countQuery.from(Baptism.class);
        Join<Baptism, Member> countMember = countRoot.join("baptizedMember", JoinType.LEFT);

        List<Predicate> countPredicates = new ArrayList<>();

        if (dto != null) {

            if (dto.getMemberId() != null) {
                countPredicates.add(cb.equal(countMember.get("id"), dto.getMemberId()));
            }

            if (dto.getStartDate() != null) {
                countPredicates.add(
                        cb.greaterThanOrEqualTo(
                                countRoot.get("date"),
                                dto.getStartDate()
                        )
                );
            }

            if (dto.getEndDate() != null) {
                countPredicates.add(
                        cb.lessThanOrEqualTo(
                                countRoot.get("date"),
                                dto.getEndDate()
                        )
                );
            }

            if (dto.isActive()) {
                countPredicates.add(cb.isFalse(countRoot.get("invalid")));
            }

            if (dto.getQuery() != null && !dto.getQuery().isBlank()) {

                String like = "%" + dto.getQuery().toLowerCase() + "%";

                Predicate p = cb.or(
                        cb.like(cb.lower(countRoot.get("neighborhood")), like),
                        cb.like(cb.lower(countRoot.get("city")), like),
                        cb.like(cb.lower(countRoot.get("municipality")), like),
                        cb.like(cb.lower(countRoot.get("district")), like),
                        cb.like(cb.lower(countRoot.get("postalCode")), like)
                );

                countPredicates.add(p);
            }
        }

        countQuery
                .select(cb.count(countRoot))
                .where(countPredicates.toArray(new Predicate[0]));

        Long total = em.createQuery(countQuery).getSingleResult();
        int totalPages = (int) Math.ceil((double) total / pageable.getPageSize());

        return BaptismPagesResponseDto.builder()
                .baptisms(results.stream()
                        .map(baptismMapper::toDto)
                        .toList())
                .pages(totalPages)
                .build();
    }

}
