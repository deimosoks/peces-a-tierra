package org.icc.pecesatierra.web.services;

import org.icc.pecesatierra.dtos.dashboard.DashboardResponseDto;
import org.icc.pecesatierra.entities.User;

public interface DashboardService {

    DashboardResponseDto dashboard(User user);

}
