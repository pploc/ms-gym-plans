package com.gym.plans.adapter.out.persistence.specification;

import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.domain.model.GymLocationStatus;
import org.springframework.data.jpa.domain.Specification;

public final class GymLocationSpecifications {

    private GymLocationSpecifications() {}

    public static Specification<GymLocationEntity> chainIdEquals(String chainId) {
        return (root, query, cb) ->
                blank(chainId) ? null : cb.equal(root.get("chainId"), chainId.trim());
    }

    public static Specification<GymLocationEntity> cityEquals(String city) {
        return (root, query, cb) -> blank(city) ? null : cb.equal(root.get("city"), city.trim());
    }

    public static Specification<GymLocationEntity> statusEquals(GymLocationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<GymLocationEntity> withFilters(
            String chainId, String city, GymLocationStatus status) {
        return Specification.where(chainIdEquals(chainId)).and(cityEquals(city)).and(statusEquals(status));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
