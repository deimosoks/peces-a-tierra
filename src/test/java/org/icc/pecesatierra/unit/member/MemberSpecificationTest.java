package org.icc.pecesatierra.unit.member;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.icc.pecesatierra.features.member.Member;
import org.icc.pecesatierra.features.member.dtos.MemberFilterRequestDto;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.specs.MemberSpecification;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * H-43: el filtro por edad es un intervalo de anios, no un punto. El limite
 * inferior de fecha (cumpleanios de {@code maxAge+1}) es exclusivo y el superior
 * (cumpleanios de {@code minAge}) inclusivo. Con el antiguo {@code between}
 * quedaba fuera quien ya tiene {@code maxAge} pero cumple {@code maxAge+1} mas
 * adelante en el anio.
 *
 * <p>No usa base de datos: captura las fechas con las que la especificacion
 * construye los predicados {@code >} y {@code <=} y comprueba la pertenencia con
 * esas mismas fechas.</p>
 */
@ExtendWith(MockitoExtension.class)
class MemberSpecificationTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Root<Member> root;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaQuery<?> query;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private CriteriaBuilder cb;

    private final MemberSpecification specification = new MemberSpecification(new DateTimeUtils());

    private final User admin = TestFixtures.user("admin-1", "admin",
            TestFixtures.member("admin-member-1", TestFixtures.branch("branch-a", "Sede A"), "Admin"),
            "ADMINISTRATOR");

    @Test
    void someoneWhoTurnsMaxAgePlusOneTodayIsExcluded_H43() {
        LocalDate today = LocalDate.now();
        Bounds bounds = ageBounds(0, 30);

        assertThat(bounds.lowerExclusive).isEqualTo(today.minusYears(31));
        assertThat(bounds.upperInclusive).isEqualTo(today.minusYears(0));

        // Cumple 31 hoy: ya no tiene 30, el limite inferior es exclusivo.
        LocalDate turns31Today = today.minusYears(31);
        assertThat(bounds.includes(turns31Today)).isFalse();
    }

    @Test
    void someoneWhoTurnsMaxAgePlusOneTomorrowStillHasMaxAgeAndIsIncluded_H43() {
        LocalDate today = LocalDate.now();
        Bounds bounds = ageBounds(0, 30);

        // Cumple 31 maniana, hoy tiene 30: antes de H-43 quedaba fuera.
        LocalDate turns31Tomorrow = today.minusYears(31).plusDays(1);
        assertThat(turns31Tomorrow).isAfter(bounds.lowerExclusive);
        assertThat(bounds.includes(turns31Tomorrow)).isTrue();
    }

    @Test
    void someoneWhoTurnsMinAgeTodayIsIncluded_H43() {
        LocalDate today = LocalDate.now();
        Bounds bounds = ageBounds(18, 30);

        // Cumple 18 hoy: entra, el limite superior es inclusivo.
        LocalDate turns18Today = today.minusYears(18);
        assertThat(bounds.upperInclusive).isEqualTo(turns18Today);
        assertThat(bounds.includes(turns18Today)).isTrue();
    }

    private Bounds ageBounds(int minAge, int maxAge) {
        MemberFilterRequestDto dto = MemberFilterRequestDto.builder()
                .ageFilterRange1(minAge)
                .ageFilterRange2(maxAge)
                .build();

        specification.build(dto, admin).toPredicate(root, query, cb);

        ArgumentCaptor<LocalDate> lower = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> upper = ArgumentCaptor.forClass(LocalDate.class);
        verify(cb).greaterThan(any(), lower.capture());
        verify(cb).lessThanOrEqualTo(any(), upper.capture());

        return new Bounds(lower.getValue(), upper.getValue());
    }

    private record Bounds(LocalDate lowerExclusive, LocalDate upperInclusive) {

        boolean includes(LocalDate birthdate) {
            return birthdate.isAfter(lowerExclusive) && !birthdate.isAfter(upperInclusive);
        }
    }
}
