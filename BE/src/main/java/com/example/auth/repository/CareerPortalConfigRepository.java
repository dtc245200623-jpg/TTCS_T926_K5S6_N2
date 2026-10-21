package com.example.auth.repository;

import com.example.auth.entity.CareerPortalConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerPortalConfigRepository extends JpaRepository<CareerPortalConfig, Long> {
    Optional<CareerPortalConfig> findTopByOrderByIdAsc();
}
