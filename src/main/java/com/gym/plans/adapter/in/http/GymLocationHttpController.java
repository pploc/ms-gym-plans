package com.gym.plans.adapter.in.http;

import com.gym.common.grpc.security.RequireRole;
import com.gym.plans.adapter.in.grpc.GrpcAccessPolicy;
import com.gym.plans.adapter.in.grpc.PlansResponseMapper;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.proto.plans.v1.CreateGymLocationRequest;
import com.gym.proto.plans.v1.GymLocationResponse;
import com.gym.proto.plans.v1.GymLocationsResponse;
import com.gym.proto.plans.v1.UpdateGymLocationRequest;
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

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @RequireRole("SUPER_ADMIN")
    public GymLocationResponse create(@RequestBody CreateGymLocationRequest request) {
        GymLocationDto dto = gymLocationService.create(
                request.getChainId(), request.getName(), request.getAddress(), request.getCity());
        return PlansResponseMapper.toGymResponse(dto);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public GymLocationResponse update(
            @PathVariable("id") String id, @RequestBody UpdateGymLocationRequest request) {
        GymLocationDto existing = gymLocationService.get(id);
        GrpcAccessPolicy.requireGym(existing.id());
        GymLocationDto dto = gymLocationService.update(
                id,
                request.getChainId(),
                request.getName(),
                request.getAddress(),
                request.getCity(),
                request.getStatus());
        return PlansResponseMapper.toGymResponse(dto);
    }

    @GetMapping("/{id}")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public GymLocationResponse get(@PathVariable("id") String id) {
        return PlansResponseMapper.toGymResponse(gymLocationService.get(id));
    }

    @GetMapping
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public GymLocationsResponse list(
            @RequestParam(value = "chain_id", required = false) String chainId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "status", required = false) String status) {
        return GymLocationsResponse.newBuilder()
                .addAllLocations(gymLocationService.list(chainId, city, status).stream()
                        .map(PlansResponseMapper::toGymResponse)
                        .toList())
                .build();
    }
}
