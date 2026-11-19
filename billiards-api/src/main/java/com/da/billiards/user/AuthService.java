package com.da.billiards.user;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.common.ApiException;
import com.da.billiards.security.JwtService;
import com.da.billiards.user.UserDtos.AuthResponse;
import com.da.billiards.user.UserDtos.ChangePasswordRequest;
import com.da.billiards.user.UserDtos.LoginRequest;
import com.da.billiards.user.UserDtos.RegisterRequest;
import com.da.billiards.user.UserDtos.UpdateProfileRequest;
import com.da.billiards.user.UserDtos.UserResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BAD_CREDENTIALS = "Email hoặc mật khẩu không đúng";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Email đã được sử dụng");
        }
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPhone(request.phone().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);
        userRepository.save(user);
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized(BAD_CREDENTIALS));
        if (!user.isEnabled()) {
            throw ApiException.unauthorized("Tài khoản đã bị khoá");
        }
        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getUser(userId);
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone().trim());
        user.setAddress(request.address() == null ? null : request.address().trim());
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Mật khẩu hiện tại không đúng");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
    }

    private AuthResponse toAuthResponse(User user) {
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.token(), token.expiresAt(), UserResponse.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
