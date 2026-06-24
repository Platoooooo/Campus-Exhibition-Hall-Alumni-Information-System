package com.campus.exhibition.controller;

import com.campus.exhibition.common.R;
import com.campus.exhibition.service.DashboardService;
import com.campus.exhibition.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作台聚合统计
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('admin') or hasAuthority('academic') or hasAuthority('college')")
    public R<DashboardVO> stats() {
        return R.ok(dashboardService.stats());
    }
}
