package org.icc.pecesatierra.unit.discipulado;

import org.icc.pecesatierra.features.discipulado.controller.DiscipuladoController;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoProgressRequestDto;
import org.icc.pecesatierra.features.discipulado.service.DiscipuladoService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DiscipuladoControllerTest {

    @Mock
    private DiscipuladoService discipuladoService;

    @InjectMocks
    private DiscipuladoController discipuladoController;

    /**
     * H-03: el controller debe propagar el {@code @AuthenticationPrincipal} al
     * servicio para que este pueda validar la sede del integrante y del maestro.
     */
    @Test
    void progressPassesAuthenticatedUserToService_H03() {
        DiscipuladoProgressRequestDto dto = DiscipuladoProgressRequestDto.builder()
                .discipuladoProgressId("progress-1")
                .teacherId("teacher-1")
                .dateCompleted(LocalDateTime.of(2026, 1, 14, 20, 0))
                .build();
        User user = TestFixtures.user("user-a", "operador-a",
                TestFixtures.member("operator-a", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));

        discipuladoController.progress(dto, user);

        verify(discipuladoService).progress(same(dto), same(user));
    }
}
