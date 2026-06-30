package com.pacto.api.auth.repository;

import com.pacto.api.auth.entity.AdvertiserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdvertiserProfileRepository extends JpaRepository<AdvertiserProfile, Long> {
}
