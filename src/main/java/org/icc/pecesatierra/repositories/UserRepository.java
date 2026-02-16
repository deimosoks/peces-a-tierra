package org.icc.pecesatierra.repositories;

import jakarta.persistence.QueryHint;
import org.icc.pecesatierra.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    @EntityGraph(attributePaths = {
            "member",
            "member.branch",
            "roles",
            "roles.role",
            "roles.role.permissions",
            "roles.role.permissions.permission"
    })
    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {
            "member",
            "roles",
            "roles.role",
            "roles.role.permissions",
            "roles.role.permissions.permission"
    })
    @Query(value = """
                SELECT DISTINCT u FROM User u
                JOIN u.member m
                WHERE
                    LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(m.completeName) LIKE LOWER(CONCAT('%', :query, '%'))
            """, countQuery = """
                SELECT COUNT(DISTINCT u) FROM User u
                JOIN u.member m
                WHERE
                    LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(m.completeName) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<User> findAll(@Param("query") String query, Pageable pageable);

    long count();

    long countByActiveTrue();

    boolean existsByMemberId(String memberId);

}
