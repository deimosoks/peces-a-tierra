package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member,String> {

    @Query("""
    SELECT m FROM Member m
    WHERE LOWER(m.completeName) LIKE LOWER(CONCAT('%', :query, '%'))
       OR m.cc LIKE CONCAT('%', :query, '%')
       OR LOWER(m.type) LIKE LOWER(CONCAT('%', :query, '%'))
       OR LOWER(m.category) LIKE LOWER(CONCAT('%', :query, '%'))
    """)
    Page<Member> findByQuery(@Param("query") String query, Pageable pageable);

    @Query("""
    SELECT m FROM Member m
    WHERE m.active = true
      AND (
           LOWER(m.completeName) LIKE LOWER(CONCAT('%', :query, '%'))
        OR m.cc LIKE CONCAT('%', :query, '%')
        OR LOWER(m.type) LIKE LOWER(CONCAT('%', :query, '%'))
        OR LOWER(m.category) LIKE LOWER(CONCAT('%', :query, '%'))
      )
    """)
    Page<Member> findByQueryActive(@Param("query") String query, Pageable pageable);

    Page<Member> findAllByActiveTrue(Pageable pageable);

}
