package com.da.billiards.security;

import com.da.billiards.user.Role;

/**
 * Thông tin người dùng đang đăng nhập, lấy từ JWT và được gắn vào SecurityContext.
 */
public record AuthUser(Long id, String email, Role role) {
}
