package tn.gpuforge.billing.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.gpuforge.billing.config.BillingProperties;
import tn.gpuforge.billing.domain.BillingException;
import tn.gpuforge.billing.domain.CreditAccount;
import tn.gpuforge.billing.domain.GpuType;
import tn.gpuforge.billing.domain.Invoice;
import tn.gpuforge.billing.domain.InvoiceStatus;
import tn.gpuforge.billing.repository.CreditAccountRepository;
import tn.gpuforge.billing.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BillingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");

    private CreditAccountRepository accounts;
    private InvoiceRepository invoices;
    private BillingService billing;

    @BeforeEach
    void setUp() {
        accounts = mock(CreditAccountRepository.class);
        invoices = mock(InvoiceRepository.class);
        PricingService pricing = new PricingService(new BillingProperties(
                new BigDecimal("0.19"), Map.of(GpuType.A100, new BigDecimal("6.500"))));
        billing = new BillingService(accounts, invoices, pricing, Clock.fixed(NOW, ZoneOffset.UTC));

        when(invoices.save(any(Invoice.class))).thenAnswer(call -> call.getArgument(0));
        when(invoices.findByJobId(any())).thenReturn(Optional.empty());
    }

    private CreditAccount givenAccount(String userId, String taxId, String balance) {
        CreditAccount account = new CreditAccount(userId, "Holder of " + userId, taxId, new BigDecimal(balance));
        when(accounts.findById(userId)).thenReturn(Optional.of(account));
        return account;
    }

    private static InvoiceRequest request(String jobId, String userId, String taxId, String gpuHours) {
        return new InvoiceRequest(jobId, userId, taxId, GpuType.A100, 1, new BigDecimal(gpuHours));
    }

    // ---------- checkCredit ----------

    @Test
    void approvesWhenTheBalanceCoversTheEstimatedCost() {
        givenAccount("alice", "1234567A", "500.000");

        CreditCheck check = billing.checkCredit("alice", GpuType.A100, new BigDecimal("2"));

        assertThat(check.approved()).isTrue();
        assertThat(check.estimatedCost()).isEqualByComparingTo("15.470");
        assertThat(check.availableCredit()).isEqualByComparingTo("500.000");
    }

    @Test
    void refusesWhenTheBalanceIsTooLow() {
        givenAccount("bob", "7654321B", "10.000");

        CreditCheck check = billing.checkCredit("bob", GpuType.A100, new BigDecimal("2"));

        assertThat(check.approved()).isFalse();
    }

    @Test
    void unknownUserIsABusinessError() {
        when(accounts.findById("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billing.checkCredit("nobody", GpuType.A100, BigDecimal.ONE))
                .isInstanceOf(BillingException.class)
                .extracting("code").isEqualTo(BillingException.Code.UNKNOWN_USER);
    }

    // ---------- createInvoice ----------

    @Test
    void invoiceIsPaidFromCreditWhenTheBalanceIsEnough() {
        CreditAccount alice = givenAccount("alice", "1234567A", "500.000");

        Invoice invoice = billing.createInvoice(request("job-1", "alice", "1234567A", "2"));

        assertThat(invoice.getId()).startsWith("INV-");
        assertThat(invoice.getAmountInclTax()).isEqualByComparingTo("15.470");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(invoice.getIssuedAt()).isEqualTo(NOW);
        assertThat(alice.getBalance()).isEqualByComparingTo("484.530");
    }

    @Test
    void invoiceStaysUnpaidAndBalanceGoesNegativeWhenCreditIsShort() {
        CreditAccount bob = givenAccount("bob", "7654321B", "10.000");

        Invoice invoice = billing.createInvoice(request("job-2", "bob", "7654321B", "2"));

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(bob.getBalance()).isEqualByComparingTo("-5.470");
    }

    @Test
    void invoicingTheSameJobTwiceReturnsTheFirstInvoiceWithoutDebitingAgain() {
        CreditAccount alice = givenAccount("alice", "1234567A", "500.000");
        Invoice first = billing.createInvoice(request("job-3", "alice", "1234567A", "2"));
        when(invoices.findByJobId("job-3")).thenReturn(Optional.of(first));

        Invoice second = billing.createInvoice(request("job-3", "alice", "1234567A", "2"));

        assertThat(second).isSameAs(first);
        assertThat(alice.getBalance()).isEqualByComparingTo("484.530");
    }

    @Test
    void refusesATaxIdThatDoesNotMatchTheAccount() {
        CreditAccount alice = givenAccount("alice", "1234567A", "500.000");

        assertThatThrownBy(() -> billing.createInvoice(request("job-4", "alice", "9999999Z", "2")))
                .isInstanceOf(BillingException.class)
                .extracting("code").isEqualTo(BillingException.Code.INVALID_TAX_ID);
        assertThat(alice.getBalance()).isEqualByComparingTo("500.000");
        verify(invoices, never()).save(any());
    }

    // ---------- getInvoice ----------

    @Test
    void unknownInvoiceIsABusinessError() {
        when(invoices.findById("INV-NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billing.getInvoice("INV-NOPE"))
                .isInstanceOf(BillingException.class)
                .extracting("code").isEqualTo(BillingException.Code.INVOICE_NOT_FOUND);
    }
}
