package com.gym.plans.adapter.out.persistence.specification;

import com.gym.plans.adapter.out.persistence.entity.MembershipPlanEntity;
import com.gym.plans.domain.model.PlanType;
import org.springframework.data.jpa.domain.Specification;

public final class MembershipPlanSpecifications {

    private MembershipPlanSpecifications() {}

    public static Specification<MembershipPlanEntity> gymIdEquals(String gymId) {
        return (root, query, cb) -> blank(gymId) ? null : cb.equal(root.get("gymId"), gymId.trim());
    }

    public static Specification<MembershipPlanEntity> planTypeEquals(PlanType planType) {
        return (root, query, cb) -> planType == null ? null : cb.equal(root.get("planType"), planType);
    }

    public static Specification<MembershipPlanEntity> activeEquals(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }

    public static Specification<MembershipPlanEntity> withFilters(
            String gymId, PlanType planType, Boolean active) {
        return Specification.where(gymIdEquals(gymId)).and(planTypeEquals(planType)).and(activeEquals(active));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
