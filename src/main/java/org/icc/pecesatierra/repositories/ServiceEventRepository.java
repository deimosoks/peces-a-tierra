package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.ServiceEvent;
import org.icc.pecesatierra.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ServiceEventRepository extends JpaRepository<ServiceEvent, String>, JpaSpecificationExecutor<ServiceEvent> {

    List<ServiceEvent>
    findByBranch_IdAndStartDateTimeLessThanEqualAndEndDateTimeGreaterThanEqual(
            String branchId,
            OffsetDateTime now1,
            OffsetDateTime now2
    );

    boolean existsByServices(Services service);

}
