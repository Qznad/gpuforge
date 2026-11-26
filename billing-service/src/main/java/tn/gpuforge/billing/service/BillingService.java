package tn.gpuforge.billing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.gpuforge.billing.domain.BillingException;
import tn.gpuforge.billing.domain.CreditAccount;
import tn.gpuforge.billing.domain.GpuType;
import tn.gpuforge.billing.domain.Invoice;
import tn.gpuforge.billing.domain.InvoiceStatus;
import tn.gpuforge.billing.domain.Quote;
import tn.gpuforge.billing.repository.CreditAccountRepository;
import tn.gpuforge.billing.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** Business rules of billing: credit checks and invoicing. */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final CreditAccountRepository accounts;
    private final InvoiceRepository invoices;
    private final PricingService pricing;
    private final Clock clock;

    public BillingService(CreditAccountRepository accounts, InvoiceRepository invoices,
                          PricingService pricing, Clock clock) {
        this.accounts = accounts;
        this.invoices = invoices;
        this.pricing = pricing;
        this.clock = clock;
    }

    /** Can the user afford a job of this size? Not having enough credit is a normal "no", not an error. */
    @Transactional(readOnly = true)
    public CreditCheck checkCredit(String userId, GpuType gpuType, BigDecimal estimatedGpuHours) {
        CreditAccount account = findAccount(userId);
        BigDecimal estimatedCost = pricing.quote(gpuType, estimatedGpuHours).amountInclTax();
        return new CreditCheck(account.canAfford(estimatedCost), estimatedCost, account.getBalance());
    }

    /**
     * Invoices a finished job and debits the customer's credit.
     * Idempotent: if the job was already invoiced, the existing invoice is returned and nothing is debited again.
     * This makes the operation safe to retry after a network failure.
     */
    @Transactional
    public Invoice createInvoice(InvoiceRequest request) {
        return invoices.findByJobId(request.jobId())
                .orElseGet(() -> issueInvoice(request));
    }

    @Transactional(readOnly = true)
    public Invoice getInvoice(String invoiceId) {
        return invoices.findById(invoiceId)
                .orElseThrow(() -> new BillingException(BillingException.Code.INVOICE_NOT_FOUND,
                        "No invoice with id '" + invoiceId + "'"));
    }

    private Invoice issueInvoice(InvoiceRequest request) {
        CreditAccount account = findAccount(request.userId());
        if (!account.hasTaxId(request.taxId())) {
            throw new BillingException(BillingException.Code.INVALID_TAX_ID,
                    "Tax ID '" + request.taxId() + "' does not match the one registered for user '"
                            + request.userId() + "'");
        }

        Quote quote = pricing.quote(request.gpuType(), request.gpuHours());
        InvoiceStatus status = account.canAfford(quote.amountInclTax()) ? InvoiceStatus.PAID : InvoiceStatus.ISSUED;
        account.debit(quote.amountInclTax());

        Invoice invoice = invoices.save(new Invoice(
                newInvoiceId(), request.jobId(), request.userId(), request.taxId(),
                request.gpuType(), request.gpuCount(), request.gpuHours(), quote,
                Instant.now(clock), status));

        log.info("Invoice {} issued for job {}: {} TND incl. VAT, status {}",
                invoice.getId(), invoice.getJobId(), invoice.getAmountInclTax(), invoice.getStatus());
        return invoice;
    }

    private CreditAccount findAccount(String userId) {
        return accounts.findById(userId)
                .orElseThrow(() -> new BillingException(BillingException.Code.UNKNOWN_USER,
                        "No credit account for user '" + userId + "'"));
    }

    private static String newInvoiceId() {
        return "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}
