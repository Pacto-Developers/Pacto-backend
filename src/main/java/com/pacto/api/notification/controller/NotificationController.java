package com.pacto.api.notification.controller;

import com.pacto.api.common.dto.PageResponse;
import com.pacto.api.common.response.CommonResponse;
import com.pacto.api.notification.dto.NotificationResponse;
import com.pacto.api.notification.dto.PushSubscriptionRequest;
import com.pacto.api.notification.dto.PushSubscriptionResponse;
import com.pacto.api.notification.service.NotificationService;
import com.pacto.api.notification.service.PushSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification", description = "앱 내 알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final PushSubscriptionService pushSubscriptionService;

    @Operation(summary = "웹푸시 등록")
    @PostMapping("/subscriptions")
    public ResponseEntity<CommonResponse<PushSubscriptionResponse>> registerPushSubscription(
            Authentication authentication,
            @RequestBody PushSubscriptionRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(CommonResponse.success(
                "웹푸시 등록 성공",
                pushSubscriptionService.register(userId, request)
        ));
    }

    @Operation(summary = "웹푸시 등록 해제")
    @DeleteMapping("/subscriptions")
    public ResponseEntity<CommonResponse<Void>> unregisterPushSubscription(
            Authentication authentication,
            @RequestBody PushSubscriptionRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        pushSubscriptionService.unregister(userId, request);
        return ResponseEntity.ok(CommonResponse.success(
                "웹푸시 등록 해제 성공",
                null
        ));
    }

    @Operation(summary = "내 알림 목록 조회")
    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(CommonResponse.success(
                "알림 목록 조회 성공",
                notificationService.getMyNotifications(userId, page, size)
        ));
    }

    @Operation(summary = "알림 읽음 처리")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<CommonResponse<NotificationResponse>> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(CommonResponse.success(
                "알림 읽음 처리 성공",
                notificationService.markAsRead(notificationId, userId)
        ));
    }
}
