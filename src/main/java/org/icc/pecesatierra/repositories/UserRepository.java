package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.domain.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

//    @Query("""
//    SELECT s FROM User s
//    WHERE s.username LIKE CONCAT('%', :query, '%')
//    """)
//    Page<User> findByQuery(@Param("query") String query, Pageable pageable);

    @Query(
            value = """
        SELECT DISTINCT u FROM User u
        JOIN u.member m
        WHERE 
            LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.completeName) LIKE LOWER(CONCAT('%', :query, '%'))
    """,
            countQuery = """
        SELECT COUNT(DISTINCT u) FROM User u
        JOIN u.member m
        WHERE 
            LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.completeName) LIKE LOWER(CONCAT('%', :query, '%'))
    """
    )
    Page<User> findAll(@Param("query") String query, Pageable pageable);



    long count();

    long countByActiveTrue();

    boolean existsByMemberId(String memberId);


}
