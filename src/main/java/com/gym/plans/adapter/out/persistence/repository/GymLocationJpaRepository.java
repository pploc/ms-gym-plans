package com.gym.plans.adapter.out.persistence.repository;

import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GymLocationJpaRepository
        extends JpaRepository<GymLocationEntity, String>, JpaSpecificationExecutor<GymLocationEntity> {}
