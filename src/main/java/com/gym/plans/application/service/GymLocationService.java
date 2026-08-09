package com.gym.plans.application.service;

import com.gym.common.error.NotFoundException;
import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.adapter.out.persistence.mapper.GymLocationMapper;
import com.gym.plans.adapter.out.persistence.repository.GymLocationJpaRepository;
import com.gym.plans.adapter.out.persistence.specification.GymLocationSpecifications;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.error.PlansDomainException;
import com.gym.plans.domain.error.PlansErrorCode;
import com.gym.plans.domain.model.GymLocationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GymLocationService {

    private final GymLocationJpaRepository gymLocationRepository;
    private final GymLocationMapper gymLocationMapper;

    @Transactional
    public GymLocationDto create(String chainId, String name, String address, String city) {
        requireText(chainId, "chain_id");
        requireText(name, "name");
        requireText(address, "address");
        requireText(city, "city");

        GymLocationEntity entity = new GymLocationEntity();
        entity.setChainId(chainId.trim());
        entity.setName(name.trim());
        entity.setAddress(address.trim());
        entity.setCity(city.trim());
        entity.setStatus(GymLocationStatus.ACTIVE);
        return gymLocationMapper.toDto(gymLocationRepository.save(entity));
    }

    @Transactional
    public GymLocationDto update(
            String id, String chainId, String name, String address, String city, String status) {
        requireText(id, "id");
        GymLocationEntity entity = requireGym(id);
        requireText(chainId, "chain_id");
        requireText(name, "name");
        requireText(address, "address");
        requireText(city, "city");

        entity.setChainId(chainId.trim());
        entity.setName(name.trim());
        entity.setAddress(address.trim());
        entity.setCity(city.trim());
        entity.setStatus(GymLocationStatus.fromWire(status));
        return gymLocationMapper.toDto(gymLocationRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public GymLocationDto get(String id) {
        return gymLocationMapper.toDto(requireGym(id));
    }

    @Transactional(readOnly = true)
    public List<GymLocationDto> list(String chainId, String city, String status) {
        GymLocationStatus parsedStatus = blank(status) ? null : GymLocationStatus.fromWire(status);
        return gymLocationRepository
                .findAll(GymLocationSpecifications.withFilters(chainId, city, parsedStatus))
                .stream()
                .map(gymLocationMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public GymLocationDto getActive(String gymId) {
        GymLocationEntity entity = requireGym(gymId);
        if (entity.getStatus() != GymLocationStatus.ACTIVE) {
            throw new PlansDomainException(PlansErrorCode.GYM_INACTIVE, "Gym is not active: " + gymId);
        }
        return gymLocationMapper.toDto(entity);
    }

    @Transactional(readOnly = true)
    public GymLocationEntity requireGym(String id) {
        requireText(id, "id");
        return gymLocationRepository
                .findById(id.trim())
                .orElseThrow(() -> new NotFoundException(PlansErrorCode.GYM_NOT_FOUND, "Gym not found: " + id));
    }

    private static void requireText(String value, String field) {
        if (blank(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
