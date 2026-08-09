package com.gym.plans.adapter.in.http.controller;

import com.gym.common.grpc.security.RequireRole;
import com.gym.plans.adapter.in.grpc.GrpcAccessPolicy;
import com.gym.plans.adapter.out.persistence.mapper.MembershipPlanMapper;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.MembershipPlanResponse;
import com.gym.proto.plans.v1.MembershipPlansResponse;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
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
    private final MembershipPlanMapper membershipPlanMapper;

    @PostMapping("/api/v1/gyms/{gym_id}/plans")
    @ResponseStatus(HttpStatus.OK)
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public MembershipPlanResponse create(
            @PathVariable("gym_id") String gymId, @RequestBody CreateMembershipPlanRequest request) {
        GrpcAccessPolicy.requireGym(gymId);
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        Boolean active = request.hasActive() ? request.getActive() : null;
        MembershipPlanDto dto = membershipPlanService.create(
                gymId,
                request.getName(),
                request.getPlanType(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                active);
        return membershipPlanMapper.toResponse(dto);
    }

    @PutMapping("/api/v1/plans/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public MembershipPlanResponse update(
            @PathVariable("id") String id, @RequestBody UpdateMembershipPlanRequest request) {
        MembershipPlanDto existing = membershipPlanService.get(id);
        GrpcAccessPolicy.requireGym(existing.gymId());
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        MembershipPlanDto dto = membershipPlanService.update(
                id,
                request.getName(),
                request.getPlanType(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                request.getActive());
        return membershipPlanMapper.toResponse(dto);
    }

    @GetMapping("/api/v1/plans/{id}")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public MembershipPlanResponse get(@PathVariable("id") String id) {
        return membershipPlanMapper.toResponse(membershipPlanService.get(id));
    }

    @GetMapping("/api/v1/gyms/{gym_id}/plans")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public MembershipPlansResponse list(
            @PathVariable("gym_id") String gymId,
            @RequestParam(value = "planType", required = false) String planType,
            @RequestParam(value = "active", required = false) Boolean active) {
        return MembershipPlansResponse.newBuilder()
                .addAllPlans(membershipPlanService.list(gymId, planType, active).stream()
                        .map(membershipPlanMapper::toResponse)
                        .toList())
                .build();
    }
}
