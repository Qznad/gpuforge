package tn.gpuforge.billing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tn.gpuforge.billing.domain.CreditAccount;
import tn.gpuforge.billing.repository.CreditAccountRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Creates two demo customers on an empty database so the service can be tried right away:
 * "alice" has plenty of credit, "bob" has very little.
 * Disable with GPUFORGE_BILLING_DEMO_DATA_ENABLED=false.
 */
@Component
@ConditionalOnProperty(prefix = "gpuforge.billing.demo-data", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final CreditAccountRepository accounts;

    public DemoDataLoader(CreditAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (accounts.count() > 0) {
            return;
        }
        accounts.saveAll(List.of(
                new CreditAccount("alice", "Alice AI Lab", "1234567A", new BigDecimal("500.000")),
                new CreditAccount("bob", "Bob Startup", "7654321B", new BigDecimal("20.000"))
        ));
        log.info("Demo credit accounts created: alice (500.000 TND), bob (20.000 TND)");
    }
}
