package tn.gpuforge.billing.service;

import tn.gpuforge.billing.domain.GpuType;

import java.math.BigDecimal;

/** What is needed to invoice a finished job. {@code gpuHours} is already multiplied by the number of GPUs. */
public record InvoiceRequest(String jobId, String userId, String taxId,
                             GpuType gpuType, int gpuCount, BigDecimal gpuHours) {
}
