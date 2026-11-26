package tn.gpuforge.billing.endpoint;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.server.endpoint.AbstractEndpointExceptionResolver;
import org.springframework.ws.server.endpoint.MethodEndpoint;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapMessage;
import tn.gpuforge.billing.domain.BillingException;
import tn.gpuforge.billing.ws.FaultCode;
import tn.gpuforge.billing.ws.FaultDetail;

import javax.xml.namespace.QName;
import java.util.Locale;

import static tn.gpuforge.billing.config.WebServiceConfig.NAMESPACE_URI;

/**
 * Turns a {@link BillingException} into a SOAP Fault (Client / Sender: the request was wrong).
 * The fault detail is the {@code <OperationName>Fault} element declared in billing.xsd, e.g.:
 *
 * <pre>{@code
 * <detail>
 *   <ns2:CheckCreditFault>
 *     <ns2:code>UNKNOWN_USER</ns2:code>
 *     <ns2:message>No credit account for user 'carol'</ns2:message>
 *   </ns2:CheckCreditFault>
 * </detail>
 * }</pre>
 *
 * Other exceptions are left to Spring-WS, which reports them as Server faults.
 */
@Component
public class BillingFaultResolver extends AbstractEndpointExceptionResolver {

    private final JAXBContext jaxb;

    public BillingFaultResolver() throws JAXBException {
        this.jaxb = JAXBContext.newInstance(FaultDetail.class);
        setOrder(Ordered.HIGHEST_PRECEDENCE);
    }

    @Override
    protected boolean resolveExceptionInternal(MessageContext messageContext, Object endpoint, Exception ex) {
        if (!(ex instanceof BillingException error)) {
            return false;
        }
        SoapMessage response = (SoapMessage) messageContext.getResponse();
        SoapFault fault = response.getSoapBody().addClientOrSenderFault(error.getMessage(), Locale.ENGLISH);

        FaultDetail detail = new FaultDetail();
        detail.setCode(FaultCode.fromValue(error.getCode().name()));
        detail.setMessage(error.getMessage());
        QName elementName = new QName(NAMESPACE_URI, faultElementName(endpoint));
        try {
            jaxb.createMarshaller().marshal(
                    new JAXBElement<>(elementName, FaultDetail.class, detail),
                    fault.addFaultDetail().getResult());
        } catch (JAXBException e) {
            throw new IllegalStateException("Cannot write SOAP fault detail", e);
        }
        return true;
    }

    /** checkCredit -> CheckCreditFault, createInvoice -> CreateInvoiceFault, ... */
    private static String faultElementName(Object endpoint) {
        String operation = endpoint instanceof MethodEndpoint method ? method.getMethod().getName() : "billing";
        return Character.toUpperCase(operation.charAt(0)) + operation.substring(1) + "Fault";
    }
}
