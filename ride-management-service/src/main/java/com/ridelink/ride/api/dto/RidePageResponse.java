package com.ridelink.ride.api.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record RidePageResponse(
        List<RideResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static RidePageResponse from(Page<RideResponse> result) {
        return new RidePageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }
}
