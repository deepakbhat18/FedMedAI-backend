package com.fedlearn.medical.controller;

import com.fedlearn.medical.dto.request.LoginRequest;
import com.fedlearn.medical.dto.request.RegisterRequest;
import com.fedlearn.medical.dto.response.ApiResponse;
import com.fedlearn.medical.dto.response.AuthResponse;
import com.fedlearn.medical.security.JwtTokenProvider;
import com.fedlearn.medical.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "User authentication & registration APIs")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @PostMapping("/register")
    @Operation(summary = "Register new user", description = "Register a new user with role ADMIN or HOSPITAL_ADMIN")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);

        ResponseCookie accessCookie = tokenProvider.generateAccessTokenCookie(response.getAccessToken());
        ResponseCookie refreshCookie = tokenProvider.generateRefreshTokenCookie(response.getRefreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate and get HttpOnly cookies + JWT response payload")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        ResponseCookie accessCookie = tokenProvider.generateAccessTokenCookie(response.getAccessToken());
        ResponseCookie refreshCookie = tokenProvider.generateRefreshTokenCookie(response.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Uses Refresh Token from HttpOnly Cookie or Header to issue a new Access Token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest request,
            @RequestParam(required = false) String refreshToken) {

        String tokenToUse = refreshToken != null ? refreshToken : tokenProvider.getRefreshTokenFromCookies(request);

        AuthResponse response = authService.refreshAccessToken(tokenToUse);

        ResponseCookie accessCookie = tokenProvider.generateAccessTokenCookie(response.getAccessToken());
        ResponseCookie refreshCookie = tokenProvider.generateRefreshTokenCookie(response.getRefreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Clears HttpOnly authentication cookies and revokes refresh tokens")
    public ResponseEntity<ApiResponse<String>> logout(
            HttpServletRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String refreshToken = tokenProvider.getRefreshTokenFromCookies(request);
        String username = userDetails != null ? userDetails.getUsername() : null;

        authService.logout(username, refreshToken);

        ResponseCookie cleanAccessCookie = tokenProvider.getCleanAccessTokenCookie();
        ResponseCookie cleanRefreshCookie = tokenProvider.getCleanRefreshTokenCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, cleanRefreshCookie.toString())
                .body(ApiResponse.success("Logged out successfully", "Session cleared"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get currently authenticated user profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserSummary>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        AuthResponse.UserSummary user = authService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}