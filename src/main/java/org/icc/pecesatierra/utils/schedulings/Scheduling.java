package org.icc.pecesatierra.utils.schedulings;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.MemberSubCategoryRepository;
import org.icc.pecesatierra.repositories.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class Scheduling {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?", zone = "America/Bogota")
    public void reorganizeMembers() {

        log.info("Iniciando reorganización de categorías...");

        int updated = memberRepository.applyCategoryRules();

        log.info("Miembros actualizados: {}", updated);
    }

    @Transactional
    @Scheduled(cron = "0 0 3 ? * 2#2")
    public void removeExpiredTokens() {
        log.info("Iniciando limpieza programada de Refresh Tokens expirados...");

        int deletedCount = refreshTokenRepository.deleteByExpiresAtBefore(new Date());

        log.info("Limpieza completada. Se eliminaron {} tokens expirados.", deletedCount);
    }

}
