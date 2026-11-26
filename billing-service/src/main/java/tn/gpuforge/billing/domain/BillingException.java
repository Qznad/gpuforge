package tn.gpuforge.billing.domain;

/**
 * A business error caused by the caller's request. It is turned into a SOAP Fault
 * whose detail carries the {@link Code} and the message.
 */
public class BillingException extends RuntimeException {

    public enum Code {
        UNKNOWN_USER,
        INVALID_TAX_ID,
        INVOICE_NOT_FOUND
    }

    private final Code code;

    public BillingException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
