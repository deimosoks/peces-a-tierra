package org.icc.pecesatierra.unit.discipulado;

import org.icc.pecesatierra.advice.GlobalHandlerException;
import org.icc.pecesatierra.features.discipulado.controller.DiscipuladoController;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoProgressRequestDto;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoRequestDto;
import org.icc.pecesatierra.features.discipulado.dtos.DiscipuladoResponseDto;
import org.icc.pecesatierra.features.discipulado.service.DiscipuladoService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DiscipuladoControllerTest {

    @Mock
    private DiscipuladoService discipuladoService;

    @InjectMocks
    private DiscipuladoController discipuladoController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(discipuladoController)
                .setControllerAdvice(new GlobalHandlerException())
                .build();
    }

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

    /**
     * H-28: {@code dateStarted} ausente debe salir del bean validation normal
     * (400), no de un {@code UnexpectedTypeException} de Hibernate Validator.
     */
    @Test
    void missingDateStartedIs400_H28() throws Exception {
        mockMvc.perform(post("/api/discipulado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"member-1\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(discipuladoService);
    }

    @Test
    void missingMemberIdIs400_H28() throws Exception {
        mockMvc.perform(post("/api/discipulado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateStarted\":\"2026-01-14T20:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(discipuladoService);
    }

    /**
     * El {@code @Valid} tambien llega al resto de {@code @RequestBody}: el
     * endpoint de progreso rechaza un DTO incompleto con 400.
     */
    @Test
    void missingProgressTeacherIdIs400_H28() throws Exception {
        mockMvc.perform(post("/api/discipulado/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"discipuladoProgressId\":\"progress-1\",\"dateCompleted\":\"2026-01-14T20:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(discipuladoService);
    }

    /**
     * Una peticion valida sigue respondiendo 201 y con la fecha no nula ya no se
     * dispara el {@code UnexpectedTypeException} de {@code @NotBlank} (H-28).
     */
    @Test
    void validCreateRequestStillReturns201_H28() throws Exception {
        when(discipuladoService.create(any(DiscipuladoRequestDto.class), any()))
                .thenReturn(DiscipuladoResponseDto.builder().id("disc-1").build());

        mockMvc.perform(post("/api/discipulado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"memberId\":\"member-1\",\"dateStarted\":\"2026-01-14T20:00:00\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("disc-1"));
    }
}
