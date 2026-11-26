package tn.gpuforge.billing.domain;

import java.math.BigDecimal;

/** The price of a number of GPU-hours: unit price, amount before tax, VAT rate and amount including VAT. */
public record Quote(BigDecimal unitPrice, BigDecimal amountExclTax, BigDecimal vatRate, BigDecimal amountInclTax) {
}
