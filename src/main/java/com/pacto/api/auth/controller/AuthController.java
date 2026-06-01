package com.pacto.api.auth.controller;

import com.pacto.api.auth.dto.LoginRequest;
import com.pacto.api.auth.dto.LoginResponse;
import com.pacto.api.auth.dto.MeResponse;
import com.pacto.api.auth.dto.SignupRequest;
import com.pacto.api.auth.service.AuthService;
import com.pacto.api.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<?>> signup(@RequestBody SignupRequest request) {

        authService.signup(request);

        return ResponseEntity.ok(CommonResponse.success("회원가입 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<LoginResponse>> login(
            @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(CommonResponse.success("로그인 성공", authService.login(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<MeResponse>> me(
            Authentication authentication
    ) {

        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                CommonResponse.success("내 정보 조회 성공", authService.getMe(userId))
        );
    }
}
