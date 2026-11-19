package com.da.billiards.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.admin.DashboardService.DashboardResponse;
import com.da.billiards.common.PageResponse;
import com.da.billiards.security.AuthUser;
import com.da.billiards.user.UserDtos.SetEnabledRequest;
import com.da.billiards.user.UserDtos.UserResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final AdminUserService adminUserService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return dashboardService.overview();
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(@RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return adminUserService.search(q, page, size);
    }

    @PatchMapping("/users/{id}/enabled")
    public UserResponse setEnabled(@AuthenticationPrincipal AuthUser admin, @PathVariable Long id,
                                   @RequestBody SetEnabledRequest request) {
        return adminUserService.setEnabled(admin.id(), id, request.enabled());
    }
}
