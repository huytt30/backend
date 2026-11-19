package com.da.billiards.user;

import java.time.Instant;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {

    private static final String PHONE_PATTERN = "^(0|\\+84)[0-9]{9,10}$";

    private UserDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100) String fullName,
            @NotBlank(message = "Vui lòng nhập email") @Email(message = "Email không hợp lệ") @Size(max = 120) String email,
            @NotBlank(message = "Vui lòng nhập số điện thoại")
            @Pattern(regexp = PHONE_PATTERN, message = "Số điện thoại không hợp lệ") String phone,
            @NotBlank(message = "Vui lòng nhập mật khẩu")
            @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự") String password) {
    }

    public record LoginRequest(
            @NotBlank(message = "Vui lòng nhập email") String email,
            @NotBlank(message = "Vui lòng nhập mật khẩu") String password) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Vui lòng nhập họ tên") @Size(max = 100) String fullName,
            @NotBlank(message = "Vui lòng nhập số điện thoại")
            @Pattern(regexp = PHONE_PATTERN, message = "Số điện thoại không hợp lệ") String phone,
            @Size(max = 255) String address) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
            @NotBlank(message = "Vui lòng nhập mật khẩu mới")
            @Size(min = 6, max = 72, message = "Mật khẩu phải từ 6 đến 72 ký tự") String newPassword) {
    }

    public record SetEnabledRequest(boolean enabled) {
    }

    public record UserResponse(Long id, String fullName, String email, String phone, String address,
                               Role role, boolean enabled, LocalDateTime createdAt) {

        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                    user.getAddress(), user.getRole(), user.isEnabled(), user.getCreatedAt());
        }
    }

    public record AuthResponse(String token, Instant expiresAt, UserResponse user) {
    }
}
