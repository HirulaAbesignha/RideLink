package com.ridelink.fare.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ridelink.fare.dto.ReceiptResponse;
import com.ridelink.fare.entity.Receipt;
import com.ridelink.fare.exception.FareApiException;
import com.ridelink.fare.repository.PaymentRepository;
import com.ridelink.fare.repository.ReceiptRepository;

@Service
public class ReceiptService {

    private final PaymentRepository paymentRepository;
    private final ReceiptRepository receiptRepository;

    public ReceiptService(
            PaymentRepository paymentRepository,
            ReceiptRepository receiptRepository) {

        this.paymentRepository = paymentRepository;
        this.receiptRepository = receiptRepository;
    }

    public ReceiptResponse getReceipt(UUID paymentId, UUID callerId, boolean admin) {

        // 1. Check whether payment exists
        var payment = paymentRepository.findById(paymentId);

        if (payment.isEmpty()) {
            throw new FareApiException(404, "PAYMENT_NOT_FOUND", "Payment not found");
        }

        // 2. Failed payment cannot have a receipt
        if (!"PAID".equals(payment.get().getStatus())) {
            throw new FareApiException(422, "RECEIPT_NOT_AVAILABLE", "Receipt is not available for a failed payment");
        }

        // 3. Find receipt
        Receipt receipt = receiptRepository
                .findByPaymentId(paymentId)
                .orElseThrow(() -> new FareApiException(404, "PAYMENT_NOT_FOUND", "Receipt not found"));

        if (!admin && !receipt.getPassengerId().equals(callerId)) {
            throw new FareApiException(403, "ACCESS_DENIED", "This receipt belongs to another passenger");
        }

        // 4. Return receipt response
        return new ReceiptResponse(
                receipt.getReceiptNumber(),
                receipt.getPaymentId(),
                receipt.getRideId(),
                receipt.getPassengerId(),
                receipt.getAmount(),
                receipt.getCurrency(),
                receipt.getPaymentStatus(),
                receipt.getMethodLabel(),
                receipt.getIssuedAt(),
                receipt.getNotice());
    }
}
