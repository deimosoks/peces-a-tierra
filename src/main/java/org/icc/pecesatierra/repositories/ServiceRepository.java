package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Services;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Services,String> {
    List<Services> findAllByActiveTrue();
}
