package tn.gpuforge.billing.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Exposes the SOAP endpoint at /ws and generates the WSDL from billing.xsd
 * at /ws/billing.wsdl (contract-first: the XSD is the source of truth).
 */
@EnableWs
@Configuration
public class WebServiceConfig {

    public static final String NAMESPACE_URI = "http://gpuforge.tn/billing/v1";

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "billing")
    public DefaultWsdl11Definition billingWsdl(XsdSchema billingSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("BillingPort");
        wsdl.setServiceName("BillingService");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace(NAMESPACE_URI);
        wsdl.setSchema(billingSchema);
        return wsdl;
    }

    @Bean
    public XsdSchema billingSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/billing.xsd"));
    }
}
