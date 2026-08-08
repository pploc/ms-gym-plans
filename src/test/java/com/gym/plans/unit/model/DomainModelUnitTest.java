package com.gym.plans.unit.model;

import com.gym.plans.domain.model.GymLocationStatus;
import com.gym.plans.domain.model.PlanType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelUnitTest {

    @Test
    void givenWireStatus_whenFromWire_thenParses() {
        assertEquals(GymLocationStatus.CLOSED, GymLocationStatus.fromWire("closed"));
        assertThrows(IllegalArgumentException.class, () -> GymLocationStatus.fromWire("INACTIVE"));
    }

    @Test
    void givenWirePlanType_whenFromWire_thenParsesAndReportsDurationNeed() {
        assertEquals(PlanType.MONTHLY, PlanType.fromWire("monthly"));
        assertTrue(PlanType.YEARLY.requiresDuration());
        assertFalse(PlanType.LIFETIME.requiresDuration());
        assertThrows(IllegalArgumentException.class, () -> PlanType.fromWire("WEEKLY"));
    }
}
