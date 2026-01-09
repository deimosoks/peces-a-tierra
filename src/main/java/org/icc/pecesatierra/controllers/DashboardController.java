package org.icc.pecesatierra.controllers;

import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.springframework.http.ResponseEntity;

public interface DashboardController {

    ResponseEntity<DashboardResponseDto> dashboard();

}
