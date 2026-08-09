package com.gym.plans.application.service;

import com.gym.common.error.NotFoundException;
import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.adapter.out.persistence.entity.MembershipPlanEntity;
import com.gym.plans.adapter.out.persistence.mapper.MembershipPlanMapper;
import com.gym.plans.adapter.out.persistence.repository.MembershipPlanJpaRepository;
import com.gym.plans.adapter.out.persistence.specification.MembershipPlanSpecifications;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.plans.domain.error.PlansDomainException;
import com.gym.plans.domain.error.PlansErrorCode;
import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanJpaRepository planRepository;
    private final GymLocationService gymLocationService;
    private final MembershipPlanMapper membershipPlanMapper;

    @Transactional
    public MembershipPlanDto create(
            String gymId,
            String name,
            String planType,
            Integer durationDays,
            long priceVnd,
            String description,
            Boolean active) {
        GymLocationEntity gym = gymLocationService.requireGym(gymId);
        PlanType type = PlanType.fromWire(planType);
        validateTerms(type, durationDays, priceVnd);
        requireText(name, "name");

        MembershipPlanEntity entity = new MembershipPlanEntity();
        entity.setGymId(gym.getId());
        entity.setName(name.trim());
        entity.setPlanType(type);
        entity.setDurationDays(normalizeDuration(type, durationDays));
        entity.setPriceVnd(priceVnd);
        entity.setDescription(description == null ? "" : description.trim());
        entity.setActive(active == null || active);
        return membershipPlanMapper.toDto(planRepository.save(entity));
    }

    @Transactional
    public MembershipPlanDto update(
            String id,
            String name,
            String planType,
            Integer durationDays,
            long priceVnd,
            String description,
            boolean active) {
        MembershipPlanEntity entity = requirePlan(id);
        PlanType type = PlanType.fromWire(planType);
        validateTerms(type, durationDays, priceVnd);
        requireText(name, "name");

        entity.setName(name.trim());
        entity.setPlanType(type);
        entity.setDurationDays(normalizeDuration(type, durationDays));
        entity.setPriceVnd(priceVnd);
        entity.setDescription(description == null ? "" : description.trim());
        entity.setActive(active);
        return membershipPlanMapper.toDto(planRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public MembershipPlanDto get(String id) {
        return membershipPlanMapper.toDto(requirePlan(id));
    }

    @Transactional(readOnly = true)
    public List<MembershipPlanDto> list(String gymId, String planType, Boolean active) {
        requireText(gymId, "gym_id");
        gymLocationService.requireGym(gymId);
        PlanType type = blank(planType) ? null : PlanType.fromWire(planType);
        return planRepository
                .findAll(MembershipPlanSpecifications.withFilters(gymId, type, active))
                .stream()
                .map(membershipPlanMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResolvedPlanDto resolvePurchasable(String planId, String gymId) {
        MembershipPlanEntity plan = requirePlan(planId);
        GymLocationEntity gym = gymLocationService.requireGym(gymId);

        if (!plan.getGymId().equals(gym.getId())) {
            throw new PlansDomainException(
                    PlansErrorCode.PLAN_GYM_MISMATCH, "Plan does not belong to gym: " + gymId);
        }
        if (gym.getStatus() != GymLocationStatus.ACTIVE) {
            throw new PlansDomainException(PlansErrorCode.GYM_INACTIVE, "Gym is not active: " + gymId);
        }
        if (!plan.isActive()) {
            throw new PlansDomainException(PlansErrorCode.PLAN_INACTIVE, "Plan is not active: " + planId);
        }

        return membershipPlanMapper.toResolvedDto(plan);
    }

    @Transactional(readOnly = true)
    public MembershipPlanEntity requirePlan(String id) {
        requireText(id, "id");
        return planRepository
                .findById(id.trim())
                .orElseThrow(() -> new NotFoundException(PlansErrorCode.PLAN_NOT_FOUND, "Plan not found: " + id));
    }

    private static void validateTerms(PlanType type, Integer durationDays, long priceVnd) {
        if (priceVnd < 0) {
            throw new IllegalArgumentException("price_vnd must be >= 0");
        }
        if (type.requiresDuration()) {
            if (durationDays == null || durationDays <= 0) {
                throw new IllegalArgumentException("duration_days must be positive for " + type);
            }
        } else if (durationDays != null) {
            throw new IllegalArgumentException("duration_days must be absent for LIFETIME");
        }
    }

    private static Integer normalizeDuration(PlanType type, Integer durationDays) {
        return type.requiresDuration() ? durationDays : null;
    }

    private static void requireText(String value, String field) {
        if (blank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
