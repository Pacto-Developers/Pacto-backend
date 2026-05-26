package com.pacto.api.escrow.repository;

import com.pacto.api.escrow.entity.EscrowLedger;
import com.pacto.api.escrow.entity.EscrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscrowLedgerRepository extends JpaRepository<EscrowLedger, Long> {
    List<EscrowLedger> findByCampaignId(Long campaignId);
    List<EscrowLedger> findByBloggerId(Long bloggerId);
    List<EscrowLedger> findByCampaignIdAndStatus(Long campaignId, EscrowStatus status);
}
