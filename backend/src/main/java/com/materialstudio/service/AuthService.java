package com.materialstudio.service;

import com.materialstudio.domain.AppUser;
import com.materialstudio.dto.AuthResponse;
import com.materialstudio.dto.UserView;
import com.materialstudio.error.ApiException;
import com.materialstudio.mapper.AppUserMapper;
import com.materialstudio.security.SessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AuthService {
    private final AppUserMapper userMapper;
    private final SessionService sessionService;

    public AuthService(AppUserMapper userMapper, SessionService sessionService) {
        this.userMapper = userMapper;
        this.sessionService = sessionService;
    }

    public AuthResponse currentState(HttpServletRequest request) {
        AppUser current = currentUser(request);
        return new AuthResponse(
                current == null ? null : UserView.from(current),
                userMapper.findAll().stream().map(UserView::from).toList()
        );
    }

    public AuthResponse loginAs(String userId) {
        AppUser selected = userMapper.findById(userId);
        if (selected == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_DEMO_USER", "演示账号不存在");
        }
        return new AuthResponse(UserView.from(selected), userMapper.findAll().stream()
                .map(UserView::from)
                .toList());
    }

    private AppUser currentUser(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String token = Arrays.stream(cookies)
                .filter(cookie -> SessionService.COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
        return sessionService.verifyToken(token)
                .map(userMapper::findById)
                .orElse(null);
    }
}
