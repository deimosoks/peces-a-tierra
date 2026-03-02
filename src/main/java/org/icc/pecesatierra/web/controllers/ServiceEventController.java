package org.icc.pecesatierra.web.controllers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.dtos.service.event.ServiceEventRequestDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventResponseDto;
import org.icc.pecesatierra.dtos.service.event.ServiceEventsFilterRequestDto;
import org.icc.pecesatierra.entities.User;
import org.icc.pecesatierra.web.services.ServiceEventService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/services-events")
public class ServiceEventController {

    private final ServiceEventService serviceEventService;

    @PostMapping
    @PreAuthorize("""
            (
            hasAuthority('REGISTER_EVENTS') 
            || 
            hasAuthority('ADMINISTRATOR')
             )
             &&
             @securityService.isActive(authentication)""")
    public ResponseEntity<ServiceEventResponseDto> create(@Valid @RequestBody ServiceEventRequestDto serviceEventRequestDto,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceEventService.create(serviceEventRequestDto, user));
    }

    @DeleteMapping("/{serviceEventId}")
    @PreAuthorize("""
            (
            hasAuthority('CANCEL_EVENTS') 
            ||
            hasAuthority('ADMINISTRATOR')
            ) 
            &&
            @securityService.isActive(authentication)""")
    public ResponseEntity<Void> cancel(@PathVariable String serviceEventId,
                                       @AuthenticationPrincipal User user) {
        serviceEventService.cancel(serviceEventId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/calendar")
    @PreAuthorize("""
            (hasAuthority('MANAGE_ATTENDANCE') 
            || 
            hasAuthority('VIEW_EVENTS_PANEL') 
            || 
            hasAuthority('ADMINISTRATOR')
             )
             && 
             @securityService.isActive(authentication)
            """)
    public ResponseEntity<List<ServiceEventResponseDto>> findForCalendar(@AuthenticationPrincipal User user,
                                                                         @RequestBody(
                                                                                 required = false
                                                                         ) ServiceEventsFilterRequestDto dto) {
        return ResponseEntity.ok(serviceEventService.findForCalendar(dto, user));
    }

    @GetMapping("/active")
    @PreAuthorize("""
            (
            hasAuthority('REGISTER_ATTENDANCE') 
            || 
            hasAuthority('ADMINISTRATOR') 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<List<ServiceEventResponseDto>> getActiveEvent(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(serviceEventService.getActiveEventForUser(user));
    }

}
