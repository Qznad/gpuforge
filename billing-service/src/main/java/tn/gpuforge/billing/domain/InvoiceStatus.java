package tn.gpuforge.billing.domain;

public enum InvoiceStatus {
    /** Issued but not fully covered by the customer's credit: the customer owes money. */
    ISSUED,
    /** Fully paid from the customer's prepaid credit. */
    PAID,
    CANCELLED
}
