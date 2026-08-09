package com.gym.plans.adapter.in.http.controller;

import com.gym.common.grpc.security.RequireRole;
import com.gym.plans.adapter.in.grpc.GrpcAccessPolicy;
import com.gym.plans.adapter.out.persistence.mapper.MembershipPlanMapper;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.shared.mapper.ProtoEnums;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.CreateMembershipPlanResponse;
import com.gym.proto.plans.v1.GetMembershipPlanResponse;
import com.gym.proto.plans.v1.ListMembershipPlansResponse;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
import com.gym.proto.plans.v1.UpdateMembershipPlanResponse;
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
    public CreateMembershipPlanResponse create(
            @PathVariable("gym_id") String gymId, @RequestBody CreateMembershipPlanRequest request) {
        GrpcAccessPolicy.requireGym(gymId);
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        Boolean active = request.hasActive() ? request.getActive() : null;
        MembershipPlanDto dto = membershipPlanService.create(
                gymId,
                request.getName(),
                ProtoEnums.toDomain(request.getPlanType()).name(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                active);
        return membershipPlanMapper.toCreateResponse(dto);
    }

    @PutMapping("/api/v1/plans/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public UpdateMembershipPlanResponse update(
            @PathVariable("id") String id, @RequestBody UpdateMembershipPlanRequest request) {
        MembershipPlanDto existing = membershipPlanService.get(id);
        GrpcAccessPolicy.requireGym(existing.gymId());
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        MembershipPlanDto dto = membershipPlanService.update(
                id,
                request.getName(),
                ProtoEnums.toDomain(request.getPlanType()).name(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                request.getActive());
        return membershipPlanMapper.toUpdateResponse(dto);
    }

    @GetMapping("/api/v1/plans/{id}")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public GetMembershipPlanResponse get(@PathVariable("id") String id) {
        return membershipPlanMapper.toGetResponse(membershipPlanService.get(id));
    }

    @GetMapping("/api/v1/gyms/{gym_id}/plans")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public ListMembershipPlansResponse list(
            @PathVariable("gym_id") String gymId,
            @RequestParam(value = "planType", required = false) String planType,
            @RequestParam(value = "active", required = false) Boolean active) {
        return ListMembershipPlansResponse.newBuilder()
                .addAllPlans(membershipPlanService.list(gymId, planType, active).stream()
                        .map(membershipPlanMapper::toGetResponse)
                        .toList())
                .build();
    }
}
