package com.gym.plans.adapter.in.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateGymLocationRequest(
        @JsonProperty("chain_id") String chainId,
        String name,
        String address,
        String city) {}
