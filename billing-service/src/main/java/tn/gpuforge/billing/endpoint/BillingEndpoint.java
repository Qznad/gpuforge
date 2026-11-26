package tn.gpuforge.billing.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;
import tn.gpuforge.billing.service.BillingService;
import tn.gpuforge.billing.service.CreditCheck;
import tn.gpuforge.billing.ws.CheckCreditRequest;
import tn.gpuforge.billing.ws.CheckCreditResponse;
import tn.gpuforge.billing.ws.CreateInvoiceRequest;
import tn.gpuforge.billing.ws.CreateInvoiceResponse;
import tn.gpuforge.billing.ws.GetInvoiceRequest;
import tn.gpuforge.billing.ws.GetInvoiceResponse;

import static tn.gpuforge.billing.config.WebServiceConfig.NAMESPACE_URI;

/**
 * SOAP endpoint of the billing service. It only translates between XML (classes generated
 * from billing.xsd) and the business layer; all rules live in {@link BillingService}.
 *
 * <p>Method names matter: a business error thrown by {@code checkCredit} is reported in a
 * {@code CheckCreditFault} detail (see {@link BillingFaultResolver}).
 */
@Endpoint
public class BillingEndpoint {

    private final BillingService billing;

    public BillingEndpoint(BillingService billing) {
        this.billing = billing;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CheckCreditRequest")
    @ResponsePayload
    public CheckCreditResponse checkCredit(@RequestPayload CheckCreditRequest request) {
        CreditCheck check = billing.checkCredit(
                request.getUserId(),
                SoapMapper.toDomain(request.getGpuType()),
                request.getEstimatedGpuHours());

        CheckCreditResponse response = new CheckCreditResponse();
        response.setApproved(check.approved());
        response.setEstimatedCost(check.estimatedCost());
        response.setAvailableCredit(check.availableCredit());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "CreateInvoiceRequest")
    @ResponsePayload
    public CreateInvoiceResponse createInvoice(@RequestPayload CreateInvoiceRequest request) {
        CreateInvoiceResponse response = new CreateInvoiceResponse();
        response.setInvoice(SoapMapper.toXml(billing.createInvoice(SoapMapper.toDomain(request))));
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "GetInvoiceRequest")
    @ResponsePayload
    public GetInvoiceResponse getInvoice(@RequestPayload GetInvoiceRequest request) {
        GetInvoiceResponse response = new GetInvoiceResponse();
        response.setInvoice(SoapMapper.toXml(billing.getInvoice(request.getInvoiceId())));
        return response;
    }
}
