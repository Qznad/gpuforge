package tn.gpuforge.billing.service;

import java.math.BigDecimal;

/** Result of a credit check: whether the job is affordable, its estimated cost incl. VAT, and the current balance. */
public record CreditCheck(boolean approved, BigDecimal estimatedCost, BigDecimal availableCredit) {
}
