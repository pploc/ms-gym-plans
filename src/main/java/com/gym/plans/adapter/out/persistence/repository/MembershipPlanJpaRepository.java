package com.gym.plans.adapter.out.persistence.repository;

import com.gym.plans.adapter.out.persistence.entity.MembershipPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MembershipPlanJpaRepository
        extends JpaRepository<MembershipPlanEntity, String>, JpaSpecificationExecutor<MembershipPlanEntity> {}
