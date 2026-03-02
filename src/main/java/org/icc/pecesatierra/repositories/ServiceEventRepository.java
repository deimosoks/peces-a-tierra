package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.ServiceEvent;
import org.icc.pecesatierra.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceEventRepository extends JpaRepository<ServiceEvent, String> {

    List<ServiceEvent>
    findByBranch_IdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
            String branchId,
            LocalDateTime now1,
            LocalDateTime now2
    );

    boolean existsByServices(Services service);

}
