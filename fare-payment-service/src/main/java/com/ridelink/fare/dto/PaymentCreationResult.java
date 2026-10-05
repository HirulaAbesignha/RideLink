package com.ridelink.fare.dto;

public record PaymentCreationResult(PaymentResponse payment, boolean replayed) { }
