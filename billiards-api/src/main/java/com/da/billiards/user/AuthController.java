package com.da.billiards.user;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.da.billiards.security.AuthUser;
import com.da.billiards.user.UserDtos.AuthResponse;
import com.da.billiards.user.UserDtos.ChangePasswordRequest;
import com.da.billiards.user.UserDtos.LoginRequest;
import com.da.billiards.user.UserDtos.RegisterRequest;
import com.da.billiards.user.UserDtos.UpdateProfileRequest;
import com.da.billiards.user.UserDtos.UserResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return authService.me(user.id());
    }

    @PutMapping("/me")
    public UserResponse updateProfile(@AuthenticationPrincipal AuthUser user,
                                      @Valid @RequestBody UpdateProfileRequest request) {
        return authService.updateProfile(user.id(), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AuthUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.id(), request);
    }
}
