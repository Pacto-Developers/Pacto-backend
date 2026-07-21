package com.pacto.api.auth.controller;

import com.pacto.api.auth.dto.LoginRequest;
import com.pacto.api.auth.dto.LoginResponse;
import com.pacto.api.auth.dto.MeResponse;
import com.pacto.api.auth.dto.ProfileImageUploadResponse;
import com.pacto.api.auth.dto.ProfileUpdateRequest;
import com.pacto.api.auth.dto.SignupRequest;
import com.pacto.api.auth.service.AuthService;
import com.pacto.api.common.response.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


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

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<ProfileImageUploadResponse>> uploadProfileImage(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {

        Long userId = (Long) authentication.getPrincipal();
        String profileImageKey = authService.uploadProfileImage(userId, file);

        return ResponseEntity.ok(
                CommonResponse.success(
                        "프로필 이미지 업로드 성공",
                        new ProfileImageUploadResponse(userId, profileImageKey)
                )
        );
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<CommonResponse<MeResponse>> updateMyProfile(
            Authentication authentication,
            @RequestBody ProfileUpdateRequest request
    ) {

        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                CommonResponse.success("내 정보 수정 성공", authService.updateMyProfile(userId, request))
        );
    }
}
