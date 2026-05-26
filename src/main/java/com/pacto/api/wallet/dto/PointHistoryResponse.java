package com.pacto.api.wallet.dto;

import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "포인트 변동 내역 응답")
public class PointHistoryResponse {

    @Schema(description = "내역 ID", example = "1")
    private final Long historyId;

    @Schema(description = "변동 금액", example = "10000")
    private final int amount;

    @Schema(description = "변동 유형", example = "CHARGE")
    private final PointHistoryType type;

    @Schema(description = "연관 ID (캠페인 등, 없을 수 있음)", example = "null")
    private final Long referenceId;

    @Schema(description = "발생 시각", example = "2026-05-26T10:00:00")
    private final LocalDateTime createdAt;

    private PointHistoryResponse(Long historyId, int amount, PointHistoryType type,
                                  Long referenceId, LocalDateTime createdAt) {
        this.historyId = historyId;
        this.amount = amount;
        this.type = type;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
    }

    public static PointHistoryResponse from(PointHistory history) {
        return new PointHistoryResponse(
                history.getHistoryId(), history.getAmount(),
                history.getType(), history.getReferenceId(), history.getCreatedAt()
        );
    }
}
