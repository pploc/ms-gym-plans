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
import com.gym.plans.shared.mapper.ProtoEnums;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.CreateGymLocationResponse;
import com.gym.proto.plans.v1.CreateMembershipPlanRequest;
import com.gym.proto.plans.v1.CreateMembershipPlanResponse;
import com.gym.proto.plans.v1.GetActiveGymRequest;
import com.gym.proto.plans.v1.GetActiveGymResponse;
import com.gym.proto.plans.v1.GetGymLocationRequest;
import com.gym.proto.plans.v1.GetGymLocationResponse;
import com.gym.proto.plans.v1.GetMembershipPlanRequest;
import com.gym.proto.plans.v1.GetMembershipPlanResponse;
import com.gym.proto.plans.v1.ListGymLocationsRequest;
import com.gym.proto.plans.v1.ListGymLocationsResponse;
import com.gym.proto.plans.v1.ListMembershipPlansRequest;
import com.gym.proto.plans.v1.ListMembershipPlansResponse;
import com.gym.proto.plans.v1.PlansServiceGrpc;
import com.gym.proto.plans.v1.ResolvePurchasablePlanRequest;
import com.gym.proto.plans.v1.ResolvePurchasablePlanResponse;
import com.gym.proto.plans.v1.UpdateGymLocationRequest;
import com.gym.proto.plans.v1.UpdateGymLocationResponse;
import com.gym.proto.plans.v1.ValidateCheckInGymRequest;
import com.gym.proto.plans.v1.ValidateCheckInGymResponse;
import com.gym.proto.plans.v1.ValidateTrainerGymRequest;
import com.gym.proto.plans.v1.ValidateTrainerGymResponse;
import com.gym.proto.plans.v1.UpdateMembershipPlanRequest;
import com.gym.proto.plans.v1.UpdateMembershipPlanResponse;
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
            CreateGymLocationRequest request, StreamObserver<CreateGymLocationResponse> responseObserver) {
        GymLocationDto dto = gymLocationService.create(
                request.getChainId(), request.getName(), request.getAddress(), request.getCity());
        complete(responseObserver, gymLocationMapper.toCreateResponse(dto));
    }

    @Override
    @RequireRole("SUPER_ADMIN")
    public void updateGymLocation(
            UpdateGymLocationRequest request, StreamObserver<UpdateGymLocationResponse> responseObserver) {
        GymLocationDto dto = gymLocationService.update(
                request.getId(),
                request.getChainId(),
                request.getName(),
                request.getAddress(),
                request.getCity(),
                ProtoEnums.toDomain(request.getStatus()).name());
        complete(responseObserver, gymLocationMapper.toUpdateResponse(dto));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void getGymLocation(GetGymLocationRequest request, StreamObserver<GetGymLocationResponse> responseObserver) {
        complete(responseObserver, gymLocationMapper.toGetResponse(gymLocationService.get(request.getId())));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void listGymLocations(
            ListGymLocationsRequest request, StreamObserver<ListGymLocationsResponse> responseObserver) {
        var locations = gymLocationService
                .list(request.getChainId(), request.getCity(), ProtoEnums.statusFilter(request.getStatus()))
                .stream()
                .map(gymLocationMapper::toGetResponse)
                .toList();
        complete(responseObserver, ListGymLocationsResponse.newBuilder().addAllLocations(locations).build());
    }

    @Override
    @RequireRole("SUPER_ADMIN")
    public void createMembershipPlan(
            CreateMembershipPlanRequest request, StreamObserver<CreateMembershipPlanResponse> responseObserver) {
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        Boolean active = request.hasActive() ? request.getActive() : null;
        MembershipPlanDto dto = membershipPlanService.create(
                request.getGymId(),
                request.getName(),
                ProtoEnums.toDomain(request.getPlanType()).name(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                active);
        complete(responseObserver, membershipPlanMapper.toCreateResponse(dto));
    }

    @Override
    @RequireRole("SUPER_ADMIN")
    public void updateMembershipPlan(
            UpdateMembershipPlanRequest request, StreamObserver<UpdateMembershipPlanResponse> responseObserver) {
        Integer duration = request.hasDurationDays() ? request.getDurationDays() : null;
        MembershipPlanDto dto = membershipPlanService.update(
                request.getId(),
                request.getName(),
                ProtoEnums.toDomain(request.getPlanType()).name(),
                duration,
                request.getPriceVnd(),
                request.getDescription(),
                request.getActive());
        complete(responseObserver, membershipPlanMapper.toUpdateResponse(dto));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void getMembershipPlan(
            GetMembershipPlanRequest request, StreamObserver<GetMembershipPlanResponse> responseObserver) {
        complete(responseObserver, membershipPlanMapper.toGetResponse(membershipPlanService.get(request.getId())));
    }

    @Override
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public void listMembershipPlans(
            ListMembershipPlansRequest request, StreamObserver<ListMembershipPlansResponse> responseObserver) {
        Boolean active = request.hasActive() ? request.getActive() : null;
        var plans = membershipPlanService
                .list(request.getGymId(), ProtoEnums.planTypeFilter(request.getPlanType()), active)
                .stream()
                .map(membershipPlanMapper::toGetResponse)
                .toList();
        complete(responseObserver, ListMembershipPlansResponse.newBuilder().addAllPlans(plans).build());
    }

    @Override
    @RequirePolicy(RpcPolicyKind.INTERNAL_WORKLOAD)
    public void getActiveGym(GetActiveGymRequest request, StreamObserver<GetActiveGymResponse> responseObserver) {
        complete(responseObserver, gymLocationMapper.toActiveResponse(gymLocationService.getActive(request.getGymId())));
    }

    @Override
    @RequirePolicy(RpcPolicyKind.INTERNAL_WORKLOAD)
    public void validateCheckInGym(
            ValidateCheckInGymRequest request, StreamObserver<ValidateCheckInGymResponse> responseObserver) {
        GymLocationDto gym = gymLocationService.getActive(request.getGymId());
        complete(responseObserver, ValidateCheckInGymResponse.newBuilder()
                .setGymId(gym.id())
                .setStatus(ProtoEnums.toProto(gym.status()))
                .build());
    }

    @Override
    @RequirePolicy(RpcPolicyKind.INTERNAL_WORKLOAD)
    public void validateTrainerGym(
            ValidateTrainerGymRequest request, StreamObserver<ValidateTrainerGymResponse> responseObserver) {
        GymLocationDto gym = gymLocationService.getActive(request.getGymId());
        complete(responseObserver, ValidateTrainerGymResponse.newBuilder()
                .setGymId(gym.id())
                .setStatus(ProtoEnums.toProto(gym.status()))
                .build());
    }

    @Override
    @RequirePolicy(RpcPolicyKind.INTERNAL_WORKLOAD)
    public void resolvePurchasablePlan(
            ResolvePurchasablePlanRequest request, StreamObserver<ResolvePurchasablePlanResponse> responseObserver) {
        ResolvedPlanDto dto =
                membershipPlanService.resolvePurchasable(request.getPlanId(), request.getGymId());
        complete(responseObserver, membershipPlanMapper.toResolvedResponse(dto));
    }

    private static <T> void complete(StreamObserver<T> observer, T response) {
        observer.onNext(response);
        observer.onCompleted();
    }
}
