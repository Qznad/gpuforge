package tn.gpuforge.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.gpuforge.billing.domain.CreditAccount;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, String> {
}
