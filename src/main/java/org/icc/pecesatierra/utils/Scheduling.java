package org.icc.pecesatierra.utils;

import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.entities.MemberCategory;
import org.icc.pecesatierra.entities.MemberSubCategory;
import org.icc.pecesatierra.repositories.MemberCategoryRepository;
import org.icc.pecesatierra.repositories.MemberRepository;
import org.icc.pecesatierra.repositories.MemberSubCategoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class Scheduling {

    private final MemberRepository memberRepository;
    private final MemberCategoryRepository memberCategoryRepository;
    private final MemberSubCategoryRepository memberSubCategoryRepository;

    @Transactional
    @Scheduled(cron = "0 0 2 * * ?", zone = "America/Bogota")
    public void reorganizationOfCategories() {

        // TODO: Implementar configuracion dinamica luego
        MemberCategory ninos = memberCategoryRepository.findById("22222222-2222-2222-2222-222222222224").get();
        MemberCategory jovenes = memberCategoryRepository.findById("22222222-2222-2222-2222-222222222223").get();

        MemberSubCategory ovejitas = memberSubCategoryRepository.findById("4747654d-0986-43e9-9d98-c200e54caadb").get();
        MemberSubCategory leon = memberSubCategoryRepository.findById("1c3e973d-2371-4940-8916-bc96b3b104f9").get();
        MemberSubCategory linaje = memberSubCategoryRepository.findById("3fb4a649-1986-440e-9081-fab2918a6284").get();

        memberRepository.updateByBirthdateRange(
                ninos.getId(), ovejitas.getId(), 0, 5
        );

        memberRepository.updateByBirthdateRange(
                ninos.getId(), leon.getId(), 6, 9
        );

        memberRepository.updateByBirthdateRange(
                ninos.getId(), linaje.getId(), 10, 14
        );

        memberRepository.updateOlderThan(
                jovenes.getId(), 14
        );
    }
}
