package tn.gpuforge.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import tn.gpuforge.billing.domain.GpuType;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Billing settings from configuration (application.yml, overridable by environment variables).
 *
 * @param vatRate VAT rate applied to every invoice, e.g. 0.19
 * @param prices  price of one GPU-hour in TND, per GPU type
 */
@ConfigurationProperties(prefix = "gpuforge.billing")
public record BillingProperties(BigDecimal vatRate, Map<GpuType, BigDecimal> prices) {

    public BigDecimal priceOf(GpuType gpuType) {
        BigDecimal price = prices.get(gpuType);
        if (price == null) {
            throw new IllegalStateException("No price configured for GPU type " + gpuType);
        }
        return price;
    }
}
