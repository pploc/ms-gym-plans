package com.gym.plans.unit.service;

import com.gym.plans.adapter.out.persistence.entity.GymLocationEntity;
import com.gym.plans.adapter.out.persistence.entity.MembershipPlanEntity;
import com.gym.plans.adapter.out.persistence.mapper.MembershipPlanMapper;
import com.gym.plans.adapter.out.persistence.repository.MembershipPlanJpaRepository;
import com.gym.plans.application.service.GymLocationService;
import com.gym.plans.application.service.MembershipPlanService;
import com.gym.plans.domain.dto.MembershipPlanDto;
import com.gym.plans.domain.model.GymLocationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MembershipPlanPriceUnitTest {

    @Mock
    private MembershipPlanJpaRepository planRepository;

    @Mock
    private GymLocationService gymLocationService;

    private final MembershipPlanMapper membershipPlanMapper = Mappers.getMapper(MembershipPlanMapper.class);
    private MembershipPlanService membershipPlanService;

    private String gymId;
    private GymLocationEntity gym;

    @BeforeEach
    void setUp() {
        membershipPlanService = new MembershipPlanService(planRepository, gymLocationService, membershipPlanMapper);
        gymId = UUID.randomUUID().toString();
        gym = new GymLocationEntity();
        gym.setId(gymId);
        gym.setStatus(GymLocationStatus.ACTIVE);
    }

    @Test
    void givenZeroPrice_whenCreate_thenPersists() {
        // Given
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);
        when(planRepository.save(any())).thenAnswer(inv -> {
            MembershipPlanEntity entity = inv.getArgument(0);
            entity.setId(UUID.randomUUID().toString());
            return entity;
        });

        // When
        MembershipPlanDto dto =
                membershipPlanService.create(gymId, "Free", "MONTHLY", 30, 0L, "promo", true);

        // Then
        assertEquals(0L, dto.priceVnd());
    }

    @Test
    void givenNegativePrice_whenCreate_thenRejects() {
        // Given
        when(gymLocationService.requireGym(gymId)).thenReturn(gym);

        // When / Then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> membershipPlanService.create(gymId, "Bad", "MONTHLY", 30, -1L, "", true));
        assertEquals("price_vnd must be >= 0", ex.getMessage());
    }
}
