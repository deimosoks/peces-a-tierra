package org.icc.pecesatierra.features.service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.icc.pecesatierra.features.service.dtos.event.ServiceEventRequestDto;
import org.icc.pecesatierra.features.service.dtos.event.ServiceEventResponseDto;
import org.icc.pecesatierra.features.service.dtos.event.ServiceEventsFilterRequestDto;
import org.icc.pecesatierra.features.service.service.ServiceEventService;
import org.icc.pecesatierra.features.user.User;
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).REGISTER_EVENTS.name())
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
             )
             &&
             @securityService.isActive(authentication)
             """)
    public ResponseEntity<ServiceEventResponseDto> create(@Valid @RequestBody ServiceEventRequestDto serviceEventRequestDto,
                                                          @AuthenticationPrincipal User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceEventService.create(serviceEventRequestDto, user));
    }

    @DeleteMapping("/{serviceEventId}")
    @PreAuthorize("""
            (
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).CANCEL_EVENTS.name()) 
            ||
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
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
            (hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).MANAGE_ATTENDANCE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).VIEW_EVENTS_PANEL.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name())
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
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).REGISTER_ATTENDANCE.name()) 
            || 
            hasAuthority(T(org.icc.pecesatierra.utils.enums.AppPermission).ADMINISTRATOR.name()) 
            )
            && 
            @securityService.isActive(authentication)
            """)
    public ResponseEntity<List<ServiceEventResponseDto>> getActiveEvent(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(serviceEventService.getActiveEventForUser(user));
    }

}
