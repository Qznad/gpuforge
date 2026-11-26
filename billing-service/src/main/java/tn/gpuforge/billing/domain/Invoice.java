package tn.gpuforge.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/** An invoice for one training job. There is at most one invoice per job. */
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String jobId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 8)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GpuType gpuType;

    @Column(nullable = false)
    private int gpuCount;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal gpuHours;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal amountExclTax;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal vatRate;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal amountInclTax;

    @Column(nullable = false)
    private Instant issuedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    protected Invoice() {
        // for JPA
    }

    public Invoice(String id, String jobId, String userId, String taxId,
                   GpuType gpuType, int gpuCount, BigDecimal gpuHours, Quote quote,
                   Instant issuedAt, InvoiceStatus status) {
        this.id = id;
        this.jobId = jobId;
        this.userId = userId;
        this.taxId = taxId;
        this.gpuType = gpuType;
        this.gpuCount = gpuCount;
        this.gpuHours = gpuHours;
        this.unitPrice = quote.unitPrice();
        this.amountExclTax = quote.amountExclTax();
        this.vatRate = quote.vatRate();
        this.amountInclTax = quote.amountInclTax();
        this.issuedAt = issuedAt;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public String getJobId() {
        return jobId;
    }

    public String getUserId() {
        return userId;
    }

    public String getTaxId() {
        return taxId;
    }

    public GpuType getGpuType() {
        return gpuType;
    }

    public int getGpuCount() {
        return gpuCount;
    }

    public BigDecimal getGpuHours() {
        return gpuHours;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getAmountExclTax() {
        return amountExclTax;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public BigDecimal getAmountInclTax() {
        return amountInclTax;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public InvoiceStatus getStatus() {
        return status;
    }
}
