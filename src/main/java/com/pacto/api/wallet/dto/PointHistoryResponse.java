package com.pacto.api.wallet.dto;

import com.pacto.api.wallet.entity.PointHistory;
import com.pacto.api.wallet.entity.PointHistoryType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PointHistoryResponse {
    private final Long historyId;
    private final int amount;
    private final PointHistoryType type;
    private final Long referenceId;
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
