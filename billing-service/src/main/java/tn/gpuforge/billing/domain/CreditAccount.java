package tn.gpuforge.billing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

/**
 * A customer's prepaid credit, in TND.
 * The balance can become negative when a job costs more than the remaining credit.
 */
@Entity
@Table(name = "credit_accounts")
public class CreditAccount {

    @Id
    private String userId;

    @Column(nullable = false)
    private String holderName;

    /** Tunisian tax ID (matricule fiscal) registered for this customer. */
    @Column(nullable = false, length = 8)
    private String taxId;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal balance;

    /** Optimistic locking: two concurrent debits cannot silently overwrite each other. */
    @Version
    private Long version;

    protected CreditAccount() {
        // for JPA
    }

    public CreditAccount(String userId, String holderName, String taxId, BigDecimal balance) {
        this.userId = userId;
        this.holderName = holderName;
        this.taxId = taxId;
        this.balance = balance;
    }

    public boolean canAfford(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Cannot debit a negative amount: " + amount);
        }
        balance = balance.subtract(amount);
    }

    public boolean hasTaxId(String candidate) {
        return taxId.equals(candidate);
    }

    public String getUserId() {
        return userId;
    }

    public String getHolderName() {
        return holderName;
    }

    public String getTaxId() {
        return taxId;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
