package com.gym.plans.unit.service;

import com.gym.common.error.NotFoundException;
import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.adapter.out.persistence.mapper.GymLocationMapper;
import com.gym.plans.adapter.out.persistence.repository.GymLocationJpaRepository;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.domain.dto.GymLocationDto;
import com.gym.plans.domain.error.PlansDomainException;
import com.gym.plans.domain.error.PlansErrorCode;
import com.gym.plans.domain.model.GymLocationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GymLocationServiceUnitTest {

    @Mock
    private GymLocationJpaRepository gymLocationRepository;

    private final GymLocationMapper gymLocationMapper = Mappers.getMapper(GymLocationMapper.class);
    private GymLocationService gymLocationService;

    private String gymId;
    private GymLocationEntity location;

    @BeforeEach
    void setUp() {
        gymLocationService = new GymLocationService(gymLocationRepository, gymLocationMapper);
        gymId = UUID.randomUUID().toString();
        location = new GymLocationEntity();
        location.setId(gymId);
        location.setChainId("chain-1");
        location.setName("Gym A");
        location.setAddress("Street 1");
        location.setCity("Hanoi");
        location.setStatus(GymLocationStatus.ACTIVE);
    }

    @Test
    void givenValidGymDetails_whenCreate_thenPersistsActiveLocation() {
        // Given
        when(gymLocationRepository.save(any())).thenAnswer(inv -> {
            GymLocationEntity entity = inv.getArgument(0);
            entity.setId(gymId);
            return entity;
        });

        // When
        GymLocationDto dto = gymLocationService.create("chain-1", "Central", "1 Main", "Hanoi");

        // Then
        assertEquals(gymId, dto.id());
        assertEquals(GymLocationStatus.ACTIVE, dto.status());
        ArgumentCaptor<GymLocationEntity> captor = ArgumentCaptor.forClass(GymLocationEntity.class);
        verify(gymLocationRepository).save(captor.capture());
        assertEquals("Central", captor.getValue().getName());
    }

    @Test
    void givenExistingGym_whenUpdate_thenReturnsUpdatedDto() {
        // Given
        when(gymLocationRepository.findById(gymId)).thenReturn(Optional.of(location));
        when(gymLocationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        GymLocationDto dto =
                gymLocationService.update(gymId, "chain-2", "Updated", "2 Main", "Saigon", "CLOSED");

        // Then
        assertEquals("Updated", dto.name());
        assertEquals(GymLocationStatus.CLOSED, dto.status());
        assertEquals("chain-2", dto.chainId());
    }

    @Test
    void givenMissingGym_whenGet_thenThrowsNotFound() {
        // Given
        when(gymLocationRepository.findById(gymId)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> gymLocationService.get(gymId));
    }

    @Test
    void givenFilters_whenList_thenDelegatesToSpecifications() {
        // Given
        when(gymLocationRepository.findAll(any(Specification.class))).thenReturn(List.of(location));

        // When
        List<GymLocationDto> result = gymLocationService.list("chain-1", "Hanoi", "ACTIVE");

        // Then
        assertEquals(1, result.size());
        assertEquals(gymId, result.getFirst().id());
        verify(gymLocationRepository).findAll(any(Specification.class));
    }

    @Test
    void givenClosedGym_whenGetActive_thenThrowsGymInactive() {
        // Given
        location.setStatus(GymLocationStatus.CLOSED);
        when(gymLocationRepository.findById(gymId)).thenReturn(Optional.of(location));

        // When
        PlansDomainException ex =
                assertThrows(PlansDomainException.class, () -> gymLocationService.getActive(gymId));

        // Then
        assertEquals(PlansErrorCode.GYM_INACTIVE, ex.getErrorCode());
    }

    @Test
    void givenBlankName_whenCreate_thenThrowsIllegalArgument() {
        assertThrows(
                IllegalArgumentException.class,
                () -> gymLocationService.create("chain-1", " ", "addr", "city"));
    }
}
