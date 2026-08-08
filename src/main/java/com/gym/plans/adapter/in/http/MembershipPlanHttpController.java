package com.gym.plans.adapter.in.http;

import com.gym.common.grpc.security.RequireRole;
import com.gym.plans.adapter.in.grpc.GrpcAccessPolicy;
import com.gym.plans.adapter.in.http.dto.CreateMembershipPlanRequest;
import com.gym.plans.adapter.in.http.dto.MembershipPlanResponse;
import com.gym.plans.adapter.in.http.dto.MembershipPlansResponse;
import com.gym.plans.adapter.in.http.dto.UpdateMembershipPlanRequest;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.MembershipPlanDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MembershipPlanHttpController {

    private final MembershipPlanService membershipPlanService;

    @PostMapping("/api/v1/gyms/{gym_id}/plans")
    @ResponseStatus(HttpStatus.OK)
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public MembershipPlanResponse create(
            @PathVariable("gym_id") String gymId, @RequestBody CreateMembershipPlanRequest request) {
        GrpcAccessPolicy.requireGym(gymId);
        long priceVnd = request.priceVnd() == null ? -1L : request.priceVnd();
        MembershipPlanDto dto = membershipPlanService.create(
                gymId,
                request.name(),
                request.planType(),
                request.durationDays(),
                priceVnd,
                request.description(),
                request.active());
        return MembershipPlanResponse.from(dto);
    }

    @PutMapping("/api/v1/plans/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public MembershipPlanResponse update(
            @PathVariable("id") String id, @RequestBody UpdateMembershipPlanRequest request) {
        MembershipPlanDto existing = membershipPlanService.get(id);
        GrpcAccessPolicy.requireGym(existing.gymId());
        boolean active = request.active() != null && request.active();
        long priceVnd = request.priceVnd() == null ? -1L : request.priceVnd();
        MembershipPlanDto dto = membershipPlanService.update(
                id,
                request.name(),
                request.planType(),
                request.durationDays(),
                priceVnd,
                request.description(),
                active);
        return MembershipPlanResponse.from(dto);
    }

    @GetMapping("/api/v1/plans/{id}")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public MembershipPlanResponse get(@PathVariable("id") String id) {
        return MembershipPlanResponse.from(membershipPlanService.get(id));
    }

    @GetMapping("/api/v1/gyms/{gym_id}/plans")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public MembershipPlansResponse list(
            @PathVariable("gym_id") String gymId,
            @RequestParam(value = "plan_type", required = false) String planType,
            @RequestParam(value = "active", required = false) Boolean active) {
        return MembershipPlansResponse.from(membershipPlanService.list(gymId, planType, active));
    }
}
