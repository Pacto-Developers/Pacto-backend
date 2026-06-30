package com.pacto.api.auth.repository;

import com.pacto.api.auth.entity.BloggerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloggerProfileRepository extends JpaRepository<BloggerProfile, Long> {
}
