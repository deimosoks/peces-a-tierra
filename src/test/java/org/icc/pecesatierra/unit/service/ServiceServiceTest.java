package org.icc.pecesatierra.unit.service;

import org.icc.pecesatierra.features.service.Services;
import org.icc.pecesatierra.features.service.dtos.ServiceRequestDto;
import org.icc.pecesatierra.features.service.dtos.ServiceResponseDto;
import org.icc.pecesatierra.features.service.exceptions.ServiceWithNameAlreadyExistsException;
import org.icc.pecesatierra.features.service.mapper.ServiceMapper;
import org.icc.pecesatierra.features.service.repository.ServiceEventRepository;
import org.icc.pecesatierra.features.service.repository.ServiceRepository;
import org.icc.pecesatierra.features.service.service.ServiceService;
import org.icc.pecesatierra.features.user.User;
import org.icc.pecesatierra.support.TestFixtures;
import org.icc.pecesatierra.utils.time.DateTimeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * H-42b: el nombre del servicio debe ser unico en alta y en edicion. Antes solo
 * lo frenaba el indice (si existia) y el handler de integridad respondia un 409
 * generico sin decir que nombre estaba repetido.
 */
@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private ServiceMapper serviceMapper;
    @Mock
    private ServiceEventRepository serviceEventRepository;
    @Mock
    private DateTimeUtils dateTimeUtils;

    @InjectMocks
    private ServiceService serviceService;

    private final User operator = TestFixtures.user("user-1", "operador-a",
            TestFixtures.member("operator-1", TestFixtures.branch("branch-a", "Sede A"), "Operador A"));

    private ServiceRequestDto request(String name) {
        return ServiceRequestDto.builder()
                .name(name)
                .description("Servicio de prueba")
                .build();
    }

    private Services service(String id, String name) {
        return Services.builder()
                .id(id)
                .name(name)
                .description("Servicio de prueba")
                .active(true)
                .createdAt(TestFixtures.FIXED_NOW)
                .build();
    }

    @Test
    void createWithAnExistingNameIsRejectedWith409_H42b() {
        when(serviceRepository.existsByName("Culto")).thenReturn(true);

        assertThatThrownBy(() -> serviceService.create(request("Culto"), operator))
                .isInstanceOfSatisfying(ServiceWithNameAlreadyExistsException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("Culto");
                });

        verify(serviceRepository, never()).save(any(Services.class));
        verifyNoInteractions(serviceMapper);
    }

    @Test
    void createWithANewNameSavesOnce_H42b() {
        when(serviceRepository.existsByName("Culto")).thenReturn(false);
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(serviceRepository.save(any(Services.class))).thenAnswer(invocation -> {
            Services services = invocation.getArgument(0);
            services.setId("service-new");
            return services;
        });
        when(serviceMapper.toDto(any(Services.class)))
                .thenReturn(ServiceResponseDto.builder().id("service-new").name("Culto").build());

        ServiceResponseDto response = serviceService.create(request("Culto"), operator);

        assertThat(response.getId()).isEqualTo("service-new");
        verify(serviceRepository).save(any(Services.class));
    }

    @Test
    void renamingToAnExistingNameIsRejectedWith409_H42b() {
        Services existing = service("service-1", "Culto");
        when(serviceRepository.findById("service-1")).thenReturn(Optional.of(existing));
        when(serviceRepository.existsByNameAndIdNot("Reunion", "service-1")).thenReturn(true);

        assertThatThrownBy(() -> serviceService.update(request("Reunion"), "service-1", operator))
                .isInstanceOfSatisfying(ServiceWithNameAlreadyExistsException.class, exception -> {
                    assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getMessage()).contains("Reunion");
                });

        assertThat(existing.getName()).isEqualTo("Culto");
        verify(serviceMapper, never()).updateEntityFromDto(any(ServiceRequestDto.class), any(Services.class));
        verify(serviceRepository, never()).save(any(Services.class));
    }

    @Test
    void keepingItsOwnNameSaves_H42b() {
        Services existing = service("service-1", "Culto");
        when(serviceRepository.findById("service-1")).thenReturn(Optional.of(existing));
        // existsByNameAndIdNot excluye el propio servicio.
        when(serviceRepository.existsByNameAndIdNot("Culto", "service-1")).thenReturn(false);
        when(dateTimeUtils.nowUTC()).thenReturn(TestFixtures.FIXED_NOW);
        when(serviceMapper.toDto(existing))
                .thenReturn(ServiceResponseDto.builder().id("service-1").name("Culto").build());

        ServiceResponseDto response = serviceService.update(request("Culto"), "service-1", operator);

        assertThat(response.getId()).isEqualTo("service-1");
        verify(serviceMapper).updateEntityFromDto(any(ServiceRequestDto.class), any(Services.class));
        verify(serviceRepository).save(existing);
    }

    @Test
    void missingServiceIsStillNotFound_H42b() {
        when(serviceRepository.findById("service-inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> serviceService.update(request("Culto"), "service-inexistente", operator))
                .isInstanceOfSatisfying(org.icc.pecesatierra.features.service.exceptions.ServicesNotFoundException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));

        // Ni siquiera se consulta la unicidad si el servicio no existe.
        verify(serviceRepository, never()).existsByNameAndIdNot(anyString(), anyString());
        verify(serviceRepository, never()).save(any(Services.class));
    }
}
