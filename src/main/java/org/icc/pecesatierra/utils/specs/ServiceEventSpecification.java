package org.icc.pecesatierra.utils.specs;

import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.service.event.ServiceEventsFilterRequestDto;
import org.icc.pecesatierra.entities.Branch;
import org.icc.pecesatierra.entities.ServiceEvent;
import org.icc.pecesatierra.entities.Services;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ServiceEventSpecification {

    private final DateTimeUtils dateTimeUtils;

    public Specification<ServiceEvent> build(ServiceEventsFilterRequestDto dto, User user) {
        return (Root<ServiceEvent> root,
                CriteriaQuery<?> query,
                CriteriaBuilder cb) -> {

            Join<ServiceEvent, Services> service = root.join("services", JoinType.INNER);
            Join<ServiceEvent, Branch> branch = root.join("branch", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                    cb.between(
                            root.get("startDateTime"),
                            dateTimeUtils.toUTC(dto.getStartDate()),
                            dateTimeUtils.toUTC(dto.getEndDate())
                    )
            );

            if (dto.getServiceId() != null) {
                predicates.add(
                        cb.equal(service.get("id"), dto.getServiceId())
                );
            }

            if (dto.getBranchId() != null && user.hasAuthority("ADMINISTRATOR")) {

                predicates.add(
                        cb.equal(branch.get("id"), dto.getBranchId())
                );

            } else if (!user.hasAuthority("ADMINISTRATOR")) {

                predicates.add(
                        cb.equal(
                                branch.get("id"),
                                user.getMember().getBranch().getId()
                        )
                );
            }

            if (query != null) {
                query.orderBy(cb.asc(root.get("startDateTime")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
