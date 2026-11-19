package com.da.billiards.admin;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.da.billiards.common.ApiException;
import com.da.billiards.common.PageResponse;
import com.da.billiards.common.Pages;
import com.da.billiards.user.User;
import com.da.billiards.user.UserDtos.UserResponse;
import com.da.billiards.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, int page, int size) {
        String keyword = q == null || q.isBlank() ? null : q.trim();
        var pageable = Pages.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.of(userRepository.search(keyword, pageable), UserResponse::from);
    }

    public UserResponse setEnabled(Long currentAdminId, Long userId, boolean enabled) {
        if (currentAdminId.equals(userId) && !enabled) {
            throw ApiException.badRequest("Không thể tự khoá tài khoản của chính mình");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản"));
        user.setEnabled(enabled);
        return UserResponse.from(user);
    }
}
