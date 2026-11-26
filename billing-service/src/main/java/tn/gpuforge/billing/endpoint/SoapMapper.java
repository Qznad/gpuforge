package tn.gpuforge.billing.endpoint;

import tn.gpuforge.billing.domain.GpuType;
import tn.gpuforge.billing.domain.Invoice;
import tn.gpuforge.billing.service.InvoiceRequest;
import tn.gpuforge.billing.ws.CreateInvoiceRequest;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;

/**
 * Converts between the XML types generated from billing.xsd (package {@code tn.gpuforge.billing.ws})
 * and the domain model. Both have classes named Invoice and GpuType, hence the qualified names.
 *
 * <p>Enums are mapped through their XML value ({@code value()} / {@code fromValue()}), not their Java
 * constant name: JAXB renames {@code A100} to the constant {@code A_100}.
 */
final class SoapMapper {

    private static final DatatypeFactory DATATYPES = newDatatypeFactory();

    private SoapMapper() {
    }

    static GpuType toDomain(tn.gpuforge.billing.ws.GpuType xml) {
        return GpuType.valueOf(xml.value());
    }

    static InvoiceRequest toDomain(CreateInvoiceRequest xml) {
        return new InvoiceRequest(
                xml.getJobId(),
                xml.getUserId(),
                xml.getTaxId(),
                toDomain(xml.getGpuType()),
                xml.getGpuCount().intValueExact(),
                xml.getGpuHours());
    }

    static tn.gpuforge.billing.ws.Invoice toXml(Invoice invoice) {
        tn.gpuforge.billing.ws.Invoice xml = new tn.gpuforge.billing.ws.Invoice();
        xml.setInvoiceId(invoice.getId());
        xml.setJobId(invoice.getJobId());
        xml.setUserId(invoice.getUserId());
        xml.setTaxId(invoice.getTaxId());
        xml.setGpuType(tn.gpuforge.billing.ws.GpuType.fromValue(invoice.getGpuType().name()));
        xml.setGpuCount(BigInteger.valueOf(invoice.getGpuCount()));
        xml.setGpuHours(invoice.getGpuHours());
        xml.setUnitPrice(invoice.getUnitPrice());
        xml.setAmountExclTax(invoice.getAmountExclTax());
        xml.setVatRate(invoice.getVatRate());
        xml.setAmountInclTax(invoice.getAmountInclTax());
        xml.setIssuedAt(toXml(invoice.getIssuedAt()));
        xml.setStatus(tn.gpuforge.billing.ws.InvoiceStatus.fromValue(invoice.getStatus().name()));
        return xml;
    }

    private static XMLGregorianCalendar toXml(Instant instant) {
        return DATATYPES.newXMLGregorianCalendar(GregorianCalendar.from(instant.atZone(ZoneOffset.UTC)));
    }

    private static DatatypeFactory newDatatypeFactory() {
        try {
            return DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("Cannot create XML DatatypeFactory", e);
        }
    }
}
