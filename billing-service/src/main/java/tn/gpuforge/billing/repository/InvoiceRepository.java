package tn.gpuforge.billing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.gpuforge.billing.domain.Invoice;

import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    Optional<Invoice> findByJobId(String jobId);
}
