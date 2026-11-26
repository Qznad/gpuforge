package tn.gpuforge.billing.service;

import org.junit.jupiter.api.Test;
import tn.gpuforge.billing.config.BillingProperties;
import tn.gpuforge.billing.domain.GpuType;
import tn.gpuforge.billing.domain.Quote;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingServiceTest {

    private final PricingService pricing = new PricingService(new BillingProperties(
            new BigDecimal("0.19"),
            Map.of(GpuType.A100, new BigDecimal("6.500"),
                    GpuType.H100, new BigDecimal("12.000"))));

    @Test
    void pricesGpuHoursAndAddsVat() {
        Quote quote = pricing.quote(GpuType.A100, new BigDecimal("2"));

        assertThat(quote.unitPrice()).isEqualByComparingTo("6.500");
        assertThat(quote.amountExclTax()).isEqualByComparingTo("13.000");
        assertThat(quote.vatRate()).isEqualByComparingTo("0.19");
        assertThat(quote.amountInclTax()).isEqualByComparingTo("15.470");
    }

    @Test
    void roundsToTheMillime() {
        // 12.000 x 0.3333 = 3.9996 -> 4.000 ; VAT 0.760 -> 4.760
        Quote quote = pricing.quote(GpuType.H100, new BigDecimal("0.3333"));

        assertThat(quote.amountExclTax()).isEqualByComparingTo("4.000");
        assertThat(quote.amountExclTax().scale()).isEqualTo(3);
        assertThat(quote.amountInclTax()).isEqualByComparingTo("4.760");
    }

    @Test
    void zeroGpuHoursCostsNothing() {
        assertThat(pricing.quote(GpuType.A100, BigDecimal.ZERO).amountInclTax()).isEqualByComparingTo("0");
    }

    @Test
    void rejectsNegativeGpuHours() {
        assertThatThrownBy(() -> pricing.quote(GpuType.A100, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failsClearlyWhenAGpuTypeHasNoPrice() {
        assertThatThrownBy(() -> pricing.quote(GpuType.RTX_4090, BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("RTX_4090");
    }
}
