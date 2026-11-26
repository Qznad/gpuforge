package tn.gpuforge.billing.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;
import tn.gpuforge.billing.domain.CreditAccount;
import tn.gpuforge.billing.repository.CreditAccountRepository;
import tn.gpuforge.billing.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.validPayload;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

/**
 * Integration test of the SOAP service: real Spring context, real endpoint, real XSD validation,
 * real JPA repositories (on an in-memory H2 database). Messages are sent as raw XML.
 */
@SpringBootTest
@ActiveProfiles("test")
class BillingEndpointIntegrationTest {

    private static final String NS = "http://gpuforge.tn/billing/v1";
    private static final Map<String, String> NAMESPACES = Map.of("b", NS);
    private static final ClassPathResource XSD = new ClassPathResource("xsd/billing.xsd");

    @Autowired
    private ApplicationContext context;
    @Autowired
    private CreditAccountRepository accounts;
    @Autowired
    private InvoiceRepository invoices;

    private MockWebServiceClient client;

    @BeforeEach
    void setUp() {
        client = MockWebServiceClient.createClient(context);
        invoices.deleteAll();
        accounts.deleteAll();
        accounts.save(new CreditAccount("alice", "Alice AI Lab", "1234567A", new BigDecimal("500.000")));
        accounts.save(new CreditAccount("bob", "Bob Startup", "7654321B", new BigDecimal("20.000")));
    }

    private static StringSource xml(String body) {
        return new StringSource(body.replace("NS", NS));
    }

    private static StringSource checkCredit(String userId, String gpuType, String hours) {
        return xml("""
                <b:CheckCreditRequest xmlns:b="NS">
                  <b:userId>%s</b:userId>
                  <b:gpuType>%s</b:gpuType>
                  <b:estimatedGpuHours>%s</b:estimatedGpuHours>
                </b:CheckCreditRequest>""".formatted(userId, gpuType, hours));
    }

    private static StringSource createInvoice(String jobId, String userId, String taxId, String hours) {
        return xml("""
                <b:CreateInvoiceRequest xmlns:b="NS">
                  <b:jobId>%s</b:jobId>
                  <b:userId>%s</b:userId>
                  <b:taxId>%s</b:taxId>
                  <b:gpuType>A100</b:gpuType>
                  <b:gpuCount>2</b:gpuCount>
                  <b:gpuHours>%s</b:gpuHours>
                </b:CreateInvoiceRequest>""".formatted(jobId, userId, taxId, hours));
    }

    // ---------- CheckCredit ----------

    @Test
    void checkCreditApprovesAnAffordableJob() throws Exception {
        client.sendRequest(withPayload(checkCredit("alice", "A100", "2")))
                .andExpect(noFault())
                .andExpect(validPayload(XSD))
                .andExpect(xpath("/b:CheckCreditResponse/b:approved", NAMESPACES).evaluatesTo("true"))
                .andExpect(xpath("/b:CheckCreditResponse/b:estimatedCost", NAMESPACES).evaluatesTo(15.47))
                .andExpect(xpath("/b:CheckCreditResponse/b:availableCredit", NAMESPACES).evaluatesTo(500.0));
    }

    @Test
    void checkCreditRefusesAJobTooExpensiveForTheBalance() {
        // 10 H100-hours = 120 + 19% = 142.800 TND > 20 TND
        client.sendRequest(withPayload(checkCredit("bob", "H100", "10")))
                .andExpect(noFault())
                // compare the text: an XPath node converted to boolean only tells whether it exists
                .andExpect(xpath("/b:CheckCreditResponse/b:approved", NAMESPACES).evaluatesTo("false"));
    }

    @Test
    void checkCreditForAnUnknownUserReturnsASoapFaultWithDetail() {
        client.sendRequest(withPayload(checkCredit("carol", "A100", "1")))
                .andExpect(clientOrSenderFault())
                .andExpect(xpath("//b:CheckCreditFault/b:code", NAMESPACES).evaluatesTo("UNKNOWN_USER"));
    }

    @Test
    void requestThatBreaksTheContractIsRejectedByXsdValidation() {
        client.sendRequest(withPayload(checkCredit("alice", "TPU_V5", "1")))
                .andExpect(clientOrSenderFault());
    }

    // ---------- CreateInvoice ----------

    @Test
    void createInvoiceComputesAmountsAndDebitsTheCredit() throws Exception {
        client.sendRequest(withPayload(createInvoice("job-100", "alice", "1234567A", "4")))
                .andExpect(noFault())
                .andExpect(validPayload(XSD))
                .andExpect(xpath("//b:invoice/b:jobId", NAMESPACES).evaluatesTo("job-100"))
                .andExpect(xpath("//b:invoice/b:amountExclTax", NAMESPACES).evaluatesTo(26.0))
                .andExpect(xpath("//b:invoice/b:amountInclTax", NAMESPACES).evaluatesTo(30.94))
                .andExpect(xpath("//b:invoice/b:status", NAMESPACES).evaluatesTo("PAID"));

        assertThat(accounts.findById("alice").orElseThrow().getBalance()).isEqualByComparingTo("469.060");
    }

    @Test
    void createInvoiceIsIdempotentForTheSameJob() {
        client.sendRequest(withPayload(createInvoice("job-200", "alice", "1234567A", "4"))).andExpect(noFault());
        client.sendRequest(withPayload(createInvoice("job-200", "alice", "1234567A", "4"))).andExpect(noFault());

        assertThat(invoices.count()).isEqualTo(1);
        assertThat(accounts.findById("alice").orElseThrow().getBalance()).isEqualByComparingTo("469.060");
    }

    @Test
    void createInvoiceWithAnotherCompanysTaxIdReturnsASoapFault() {
        client.sendRequest(withPayload(createInvoice("job-300", "alice", "7654321B", "1")))
                .andExpect(clientOrSenderFault())
                .andExpect(xpath("//b:CreateInvoiceFault/b:code", NAMESPACES).evaluatesTo("INVALID_TAX_ID"));
    }

    // ---------- GetInvoice ----------

    @Test
    void getInvoiceReturnsAnExistingInvoice() throws Exception {
        client.sendRequest(withPayload(createInvoice("job-400", "alice", "1234567A", "1"))).andExpect(noFault());
        String invoiceId = invoices.findByJobId("job-400").orElseThrow().getId();

        client.sendRequest(withPayload(xml("""
                        <b:GetInvoiceRequest xmlns:b="NS"><b:invoiceId>%s</b:invoiceId></b:GetInvoiceRequest>
                        """.formatted(invoiceId))))
                .andExpect(noFault())
                .andExpect(validPayload(XSD))
                .andExpect(xpath("//b:invoice/b:invoiceId", NAMESPACES).evaluatesTo(invoiceId));
    }

    @Test
    void getInvoiceForAnUnknownIdReturnsASoapFault() {
        client.sendRequest(withPayload(xml("""
                        <b:GetInvoiceRequest xmlns:b="NS"><b:invoiceId>INV-NOPE</b:invoiceId></b:GetInvoiceRequest>
                        """)))
                .andExpect(clientOrSenderFault())
                .andExpect(xpath("//b:GetInvoiceFault/b:code", NAMESPACES).evaluatesTo("INVOICE_NOT_FOUND"));
    }
}
