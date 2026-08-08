package com.gym.plans.adapter.in.http;

import com.gym.common.grpc.security.RequireRole;
import com.gym.plans.adapter.in.grpc.GrpcAccessPolicy;
import com.gym.plans.adapter.in.http.dto.GymLocationHttpDtos.CreateGymLocationRequest;
import com.gym.plans.adapter.in.http.dto.GymLocationHttpDtos.GymLocationResponse;
import com.gym.plans.adapter.in.http.dto.GymLocationHttpDtos.GymLocationsResponse;
import com.gym.plans.adapter.in.http.dto.GymLocationHttpDtos.UpdateGymLocationRequest;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.domain.dto.GymLocationDto;
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
                request.chainId(), request.name(), request.address(), request.city());
        return GymLocationResponse.from(dto);
    }

    @PutMapping("/{id}")
    @RequireRole({"ADMIN", "SUPER_ADMIN"})
    public GymLocationResponse update(
            @PathVariable("id") String id, @RequestBody UpdateGymLocationRequest request) {
        GymLocationDto existing = gymLocationService.get(id);
        GrpcAccessPolicy.requireGym(existing.id());
        GymLocationDto dto = gymLocationService.update(
                id, request.chainId(), request.name(), request.address(), request.city(), request.status());
        return GymLocationResponse.from(dto);
    }

    @GetMapping("/{id}")
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public GymLocationResponse get(@PathVariable("id") String id) {
        return GymLocationResponse.from(gymLocationService.get(id));
    }

    @GetMapping
    @RequireRole({"CUSTOMER", "TRAINER", "ADMIN", "SUPER_ADMIN"})
    public GymLocationsResponse list(
            @RequestParam(value = "chain_id", required = false) String chainId,
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "status", required = false) String status) {
        return GymLocationsResponse.from(gymLocationService.list(chainId, city, status));
    }
}
