package com.restaurant.controller;

import com.restaurant.dto.ApiResponse;
import com.restaurant.dto.AuthDtos;
import com.restaurant.security.CookieService;
import com.restaurant.security.RestaurantUserDetails;
import com.restaurant.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CookieService cookies;

    public AuthController(AuthService authService, CookieService cookies) {
        this.authService = authService;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    public org.springframework.http.ResponseEntity<ApiResponse<AuthDtos.UserResponse>> register(
            @Valid @RequestBody AuthDtos.RegisterRequest request,
            HttpServletResponse response
    ) {
        AuthDtos.AuthResult result = authService.register(request);
        cookies.write(response, result.token());
        return org.springframework.http.ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result.user()));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.UserResponse> login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletResponse response
    ) {
        AuthDtos.AuthResult result = authService.login(request);
        cookies.write(response, result.token());
        return ApiResponse.ok(result.user());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal RestaurantUserDetails user,
            HttpServletResponse response
    ) {
        authService.logout(user.getId());
        cookies.clear(response);
        return ApiResponse.message(null, "Signed out.");
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.UserResponse> me(@AuthenticationPrincipal RestaurantUserDetails user) {
        return ApiResponse.ok(authService.me(user.getId()));
    }
}
