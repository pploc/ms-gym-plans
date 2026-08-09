package com.gym.plans.adapter.in.grpc;

import com.gym.common.grpc.security.RequirePolicy;
import com.gym.common.grpc.security.RequireRole;
import com.gym.common.grpc.security.RpcPolicyKind;
import com.gym.plans.adapter.out.persistence.mapper.GymLocationMapper;
import com.gym.plans.adapter.out.persistence.mapper.MembershipPlanMapper;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.dto.ResolvedPlanDto;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.GetActiveGymRequest;
import com.gym.proto.plans.v1.GetGymLocationRequest;
import com.gym.proto.plans.v1.GetMembershipPlanRequest;
import com.gym.proto.plans.v1.GymLocationResponse;
import com.gym.proto.plans.v1.GymLocationsResponse;
import com.gym.proto.plans.v1.ListGymLocationsRequest;
import com.gym.proto.plans.v1.ListMembershipPlansRequest;
import com.gym.proto.plans.v1.MembershipPlanResponse;
import com.gym.proto.plans.v1.MembershipPlansResponse;
import com.gym.proto.plans.v1.PlansServiceGrpc;
import com.gym.proto.plans.v1.ResolvePurchasablePlanRequest;
import com.gym.proto.plans.v1.ResolvedPlanResponse;
import com.gym.proto.plans.v1.UpdateGymLocationRequest;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlansGrpcHandler extends PlansServiceGrpc.PlansServiceImplBase {

    private final GymLocationService gymLocationService;
    private final MembershipPlanService membershipPlanService;
    private final GymLocationMapper gymLocationMapper;
    private final MembershipPlanMapper membershipPlanMapper;

    @Override
    @RequireRole("SUPER_ADMIN")
    public void createGymLocation(
            CreateGymLocationRequest request, StreamObserver<GymLocationResponse> responseObserver) {
        GymLocationDto dto = gymLocationService.create(
                request.getChainId(), request.getName(), request.getAddress(), request.getCity());
        complete(responseObserver, gymLocationMapper.toResponse(dto));
    }

    @Override
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public void updateGymLocation(
            UpdateGymLocationRequest request, StreamObserver<GymLocationResponse> responseObserver) {
        GymLocationDto existing = gymLocationService.get(request.getId());
        GrpcAccessPolicy.requireGym(existing.id());
        GymLocationDto dto = gymLocationService.update(
                request.getId(),
                request.getChainId(),
                request.getName(),
                request.getAddress(),
                request.getCity(),
                request.getStatus());
        complete(responseObserver, gymLocationMapper.toResponse(dto));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void getGymLocation(GetGymLocationRequest request, StreamObserver<GymLocationResponse> responseObserver) {
        complete(responseObserver, gymLocationMapper.toResponse(gymLocationService.get(request.getId())));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void listGymLocations(
            ListGymLocationsRequest request, StreamObserver<GymLocationsResponse> responseObserver) {
        var locations = gymLocationService
                .list(request.getChainId(), request.getCity(), request.getStatus())
                .stream()
                .map(gymLocationMapper::toResponse)
                .toList();
        complete(responseObserver, GymLocationsResponse.newBuilder().addAllLocations(locations).build());
    }

    @Override
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public void createMembershipPlan(
            CreateMembershipPlanRequest request, StreamObserver<MembershipPlanResponse> responseObserver) {
        GrpcAccessPolicy.requireGym(request.getGymId());
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        Boolean active = request.hasActive() ? request.getActive() : null;
        MembershipPlanDto dto = membershipPlanService.create(
                request.getGymId(),
                request.getName(),
                request.getPlanType(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                active);
        complete(responseObserver, membershipPlanMapper.toResponse(dto));
    }

    @Override
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public void updateMembershipPlan(
            UpdateMembershipPlanRequest request, StreamObserver<MembershipPlanResponse> responseObserver) {
        MembershipPlanDto existing = membershipPlanService.get(request.getId());
        GrpcAccessPolicy.requireGym(existing.gymId());
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        MembershipPlanDto dto = membershipPlanService.update(
                request.getId(),
                request.getName(),
                request.getPlanType(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                request.getActive());
        complete(responseObserver, membershipPlanMapper.toResponse(dto));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void getMembershipPlan(
            GetMembershipPlanRequest request, StreamObserver<MembershipPlanResponse> responseObserver) {
        complete(responseObserver, membershipPlanMapper.toResponse(membershipPlanService.get(request.getId())));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void listMembershipPlans(
            ListMembershipPlansRequest request, StreamObserver<MembershipPlansResponse> responseObserver) {
        Boolean active = request.hasActive() ? request.getActive() : null;
        var plans = membershipPlanService
                .list(request.getGymId(), request.getPlanType(), active)
                .stream()
                .map(membershipPlanMapper::toResponse)
                .toList();
        complete(responseObserver, MembershipPlansResponse.newBuilder().addAllPlans(plans).build());
    }

    @Override
    @RequirePolicy(RpcPolicyKind.INTERNAL_WORKLOAD)
    public void getActiveGym(GetActiveGymRequest request, StreamObserver<GymLocationResponse> responseObserver) {
        complete(responseObserver, gymLocationMapper.toResponse(gymLocationService.getActive(request.getGymId())));
    }

    @Override
    @RequirePolicy(RpcPolicyKind.INTERNAL_WORKLOAD)
    public void resolvePurchasablePlan(
            ResolvePurchasablePlanRequest request, StreamObserver<ResolvedPlanResponse> responseObserver) {
        ResolvedPlanDto dto =
                membershipPlanService.resolvePurchasable(request.getPlanId(), request.getGymId());
        complete(responseObserver, membershipPlanMapper.toResolvedResponse(dto));
    }

    private static <T> void complete(StreamObserver<T> observer, T response) {
        observer.onNext(response);
        observer.onCompleted();
    }
}
