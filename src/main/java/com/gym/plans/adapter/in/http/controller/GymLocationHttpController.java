package com.gym.plans.adapter.in.http.controller;

import com.gym.common.grpc.security.RequireRole;
import com.gym.plans.adapter.out.persistence.mapper.GymLocationMapper;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.shared.mapper.ProtoEnums;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.CreateGymLocationResponse;
import com.gym.proto.plans.v1.GetGymLocationResponse;
import com.gym.proto.plans.v1.ListGymLocationsResponse;
import com.gym.proto.plans.v1.UpdateGymLocationRequest;
import com.gym.proto.plans.v1.UpdateGymLocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gyms")
@RequiredArgsConstructor
public class GymLocationHttpController {

    private final GymLocationService gymLocationService;
    private final GymLocationMapper gymLocationMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @RequireRole("SUPER_ADMIN")
    public CreateGymLocationResponse create(@RequestBody CreateGymLocationRequest request) {
        GymLocationDto dto = gymLocationService.create(
                request.getChainId(), request.getName(), request.getAddress(), request.getCity());
        return gymLocationMapper.toCreateResponse(dto);
    }

    @PutMapping("/{id}")
    @RequireRole("SUPER_ADMIN")
    public UpdateGymLocationResponse update(
            @PathVariable("id") String id, @RequestBody UpdateGymLocationRequest request) {
        GymLocationDto dto = gymLocationService.update(
                id,
                request.getChainId(),
                request.getName(),
                request.getAddress(),
                request.getCity(),
                ProtoEnums.toDomain(request.getStatus()).name());
        return gymLocationMapper.toUpdateResponse(dto);
    }

    @GetMapping("/{id}")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public GetGymLocationResponse get(@PathVariable("id") String id) {
        return gymLocationMapper.toGetResponse(gymLocationService.get(id));
    }

    @GetMapping
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public ListGymLocationsResponse list(
            @RequestParam(value = "chainId", required = false) String chainId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "status", required = false) String status) {
        return ListGymLocationsResponse.newBuilder()
                .addAllLocations(gymLocationService.list(chainId, city, status).stream()
                        .map(gymLocationMapper::toGetResponse)
                        .toList())
                .build();
    }
}
