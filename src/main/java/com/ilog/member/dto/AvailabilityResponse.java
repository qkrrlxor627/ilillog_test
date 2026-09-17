package com.ilog.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record AvailabilityResponse(@Schema(example = "true") boolean available) {}
