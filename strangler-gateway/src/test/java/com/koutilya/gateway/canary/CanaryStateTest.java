package com.koutilya.gateway.canary;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CanaryStateTest {

    @Test
    void zeroWeightNeverRoutesToNewService() {
        CanaryState state = new CanaryState(0);
        for (int i = 0; i < 100; i++) {
            assertThat(state.routeToNewService(null)).isFalse();
        }
    }

    @Test
    void fullWeightAlwaysRoutesToNewService() {
        CanaryState state = new CanaryState(100);
        for (int i = 0; i < 100; i++) {
            assertThat(state.routeToNewService(null)).isTrue();
        }
    }

    @Test
    void weightIsClampedToValidRange() {
        CanaryState state = new CanaryState(0);
        state.setNewServiceWeight(250, "test");
        assertThat(state.getNewServiceWeight()).isEqualTo(100);
        state.setNewServiceWeight(-40, "test");
        assertThat(state.getNewServiceWeight()).isEqualTo(0);
    }

    @Test
    void rollbackForcesWeightToZeroAndSetsFlag() {
        CanaryState state = new CanaryState(90);
        state.rollback("slo breach");
        assertThat(state.getNewServiceWeight()).isZero();
        assertThat(state.isRolledBack()).isTrue();
        assertThat(state.getLastChangeReason()).contains("slo breach");
    }

    @Test
    void settingWeightClearsRolledBackFlag() {
        CanaryState state = new CanaryState(0);
        state.rollback("breach");
        state.setNewServiceWeight(10, "resume canary");
        assertThat(state.isRolledBack()).isFalse();
        assertThat(state.getNewServiceWeight()).isEqualTo(10);
    }
}
