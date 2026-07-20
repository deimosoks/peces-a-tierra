package org.icc.pecesatierra.features.service.repository;

import org.icc.pecesatierra.features.service.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Services,String> {
    List<Services> findAllByActiveTrue();
}
