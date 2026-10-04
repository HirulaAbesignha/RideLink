package com.ridelink.fare.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ridelink.fare.client.RideServiceClient;
import com.ridelink.fare.dto.PaymentRequest;
import com.ridelink.fare.dto.PaymentCreationResult;
import com.ridelink.fare.dto.PaymentResponse;
import com.ridelink.fare.dto.RideSummaryResponse;
import com.ridelink.fare.entity.Payment;
import com.ridelink.fare.entity.Receipt;
import com.ridelink.fare.exception.FareApiException;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;

@Service
public class PaymentService {

    private static final String CURRENCY = "LKR";

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;
    private final RideServiceClient rideServiceClient;

    public PaymentService(
            PaymentRepository paymentRepository,
            ReceiptRepository receiptRepository,
            RideServiceClient rideServiceClient) {

        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
        this.rideServiceClient = rideServiceClient;
    }

    @Transactional
    public PaymentCreationResult createPayment(
            PaymentRequest request,
            String idempotencyKey,
            UUID passengerId) {

        var existingPayment =
                paymentRepository.findByIdempotencyKey(idempotencyKey);

        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            if (payment.getPassengerId() == null || !payment.getPassengerId().equals(passengerId)) {
                throw new FareApiException(403, "ACCESS_DENIED", "This idempotency key belongs to another passenger");
            }
            String requestedStatus = "SUCCESS".equalsIgnoreCase(request.simulationOutcome()) ? "PAID" : "FAILED";
            if (!payment.getRideId().equals(request.rideId())
                    || !payment.getMethodLabel().equals(request.methodLabel())
                    || !payment.getStatus().equals(requestedStatus)) {
                throw new FareApiException(409, "IDEMPOTENCY_CONFLICT",
                        "This idempotency key was already used with a different request");
            }
            return new PaymentCreationResult(toResponse(payment), true);
        }

        RideSummaryResponse ride =
                rideServiceClient.getRideSummary(request.rideId());

        if (ride == null) {
            throw new FareApiException(503, "RIDE_SERVICE_UNAVAILABLE", "Ride Service returned no ride summary");
        }
        if (!ride.passengerId().equals(passengerId)) {
            throw new FareApiException(403, "ACCESS_DENIED", "Only the ride passenger may make this payment");
        }
        if (!"COMPLETED".equalsIgnoreCase(ride.status())) {
            throw new FareApiException(422, "RIDE_NOT_COMPLETED", "Payment is allowed only for completed rides");
        }
        if (ride.finalFare() == null || ride.finalFare().signum() <= 0) {
            throw new FareApiException(422, "FINAL_FARE_NOT_FOUND", "The completed ride has no valid final fare");
        }

        var existingRidePayment = paymentRepository
                .findFirstByRideIdAndStatusOrderByRecordedAtDesc(request.rideId(), "PAID");

        if (existingRidePayment.isPresent()) {

            throw new FareApiException(409, "RIDE_ALREADY_PAID", "Ride already has a successful payment");
        }

        Payment payment = new Payment();

        payment.setPaymentId(UUID.randomUUID());
        payment.setRideId(ride.rideId());
        payment.setPassengerId(ride.passengerId());

        payment.setAmount(
                ride.finalFare());

        payment.setCurrency(CURRENCY);

        if ("SUCCESS".equalsIgnoreCase(request.simulationOutcome())) {
            payment.setStatus("PAID");
            payment.setSuccessfulRideId(ride.rideId());
        } else {
            payment.setStatus("FAILED");
        }

        payment.setMethodLabel(request.methodLabel());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setRecordedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);

        if ("PAID".equals(savedPayment.getStatus())) {
            createReceipt(savedPayment, ride);
        }

        return new PaymentCreationResult(toResponse(savedPayment), false);
    }

    private void createReceipt(
            Payment payment,
            RideSummaryResponse ride) {

        Receipt receipt = new Receipt();

        receipt.setReceiptId(UUID.randomUUID());

        String date = java.time.LocalDate.now(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String paymentSuffix = payment.getPaymentId().toString().substring(0, 8).toUpperCase();
        receipt.setReceiptNumber("RL-" + date + "-" + paymentSuffix);

        receipt.setPaymentId(payment.getPaymentId());
        receipt.setRideId(payment.getRideId());
        receipt.setPassengerId(ride.passengerId());
        receipt.setAmount(payment.getAmount());
        receipt.setCurrency(payment.getCurrency());
        receipt.setPaymentStatus(payment.getStatus());
        receipt.setMethodLabel(payment.getMethodLabel());
        receipt.setIssuedAt(Instant.now());

        receipt.setNotice("Simulated payment - no real money was transferred");

        receiptRepository.save(receipt);
    }

    private PaymentResponse toResponse(Payment payment) {

        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getRideId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getMethodLabel(),
                payment.getRecordedAt());
    }
}
