package org.icc.pecesatierra.repositories;

import org.icc.pecesatierra.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    void deleteByToken(String token);

    @Modifying
    @Query("""
            DELETE FROM RefreshToken r
            WHERE r.token = :token
            AND r.expiresAt > :now
            """)
    int deleteIfValid(
            @Param("token") String token,
            @Param("now") Date now
    );

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Date now);

}
