package org.icc.pecesatierra.controllers.imp;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.icc.pecesatierra.controllers.ServiceController;
import org.icc.pecesatierra.dtos.service.ServiceRequestDto;
import org.icc.pecesatierra.dtos.service.ServiceResponseDto;
import org.icc.pecesatierra.services.ServiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/services")
public class ServiceControllerImp implements ServiceController {

    private final ServiceService serviceService;

    @Override
    @PostMapping
    public ResponseEntity<ServiceResponseDto> create(@Valid @RequestBody ServiceRequestDto serviceRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.create(serviceRequestDto));
    }

    @Override
    @PutMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDto> update(@Valid @RequestBody ServiceRequestDto serviceRequestDto,
                                                     @PathVariable String serviceId) {
        return ResponseEntity.ok(serviceService.update(serviceRequestDto, serviceId));
    }

    @Override
    @DeleteMapping("/{serviceId}")
    public ResponseEntity<ServiceResponseDto> delete(@PathVariable String serviceId) {
        serviceService.delete(serviceId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<List<ServiceResponseDto>> findAll(@RequestParam boolean onlyActive) {
        return ResponseEntity.ok(serviceService.findAll(onlyActive));
    }

    @Override
    @PatchMapping("/{serviceId}")
    public ResponseEntity<Boolean> updateState(@PathVariable String serviceId,
                                               @RequestParam boolean active) {
        return ResponseEntity.ok(serviceService.updateActive(serviceId, active));
    }
}
// service eb496949-6443-4f70-9bcf-fca965740084
// member 007e5ee8-8c56-489c-9c59-f27379950271