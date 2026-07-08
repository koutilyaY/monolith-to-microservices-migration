package com.koutilya.gateway.canary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Operator control plane for the canary. Lets an operator (or a deploy pipeline) advance the
 * weight 0 -> 10 -> 50 -> 100, inspect current state, and trigger an instant manual rollback.
 */
@RestController
@RequestMapping("/admin/canary")
public class CanaryController {

    private final CanaryState canaryState;

    public CanaryController(CanaryState canaryState) {
        this.canaryState = canaryState;
    }

    @GetMapping
    public Map<String, Object> status() {
        return Map.of(
                "ordersServiceWeight", canaryState.getNewServiceWeight(),
                "rolledBack", canaryState.isRolledBack(),
                "lastChangeReason", canaryState.getLastChangeReason());
    }

    @PostMapping("/weight/{weight}")
    public Map<String, Object> setWeight(@PathVariable int weight) {
        canaryState.setNewServiceWeight(weight, "manual weight change via admin API");
        return status();
    }

    @PostMapping("/rollback")
    public Map<String, Object> rollback() {
        canaryState.rollback("manual rollback via admin API");
        return status();
    }
}
