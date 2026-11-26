package tn.gpuforge.billing.service;

import org.springframework.stereotype.Service;
import tn.gpuforge.billing.config.BillingProperties;
import tn.gpuforge.billing.domain.GpuType;
import tn.gpuforge.billing.domain.Quote;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes prices: GPU-hours x hourly price of the GPU type, plus VAT.
 * Amounts are rounded to 3 decimals (the millime, smallest unit of the Tunisian dinar).
 */
@Service
public class PricingService {

    private static final int MONEY_SCALE = 3;

    private final BillingProperties properties;

    public PricingService(BillingProperties properties) {
        this.properties = properties;
    }

    public Quote quote(GpuType gpuType, BigDecimal gpuHours) {
        if (gpuHours.signum() < 0) {
            throw new IllegalArgumentException("GPU-hours cannot be negative: " + gpuHours);
        }
        BigDecimal unitPrice = properties.priceOf(gpuType);
        BigDecimal exclTax = unitPrice.multiply(gpuHours).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal vat = exclTax.multiply(properties.vatRate()).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new Quote(
                unitPrice.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                exclTax,
                properties.vatRate(),
                exclTax.add(vat));
    }
}
