package org.apache.cxf.transport.play;

import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.interceptor.*;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
 
public class WSSecurityNamespaceInInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(WSSecurityNamespaceInInterceptor.class);
 
    public WSSecurityNamespaceInInterceptor() {
        super(Phase.PRE_PROTOCOL);
        addAfter(ReadHeadersInterceptor.class.getName());
        addAfter(EndpointSelectionInterceptor.class.getName());
    }
 
    public void handleMessage(SoapMessage message) throws Fault {
        List<Header> headers = message.getHeaders();
        LOG.info("Obtained headers.");
        if (headers != null) {
            for (Header header : headers) {
                LOG.info("Checking header: " + header.getName().getLocalPart() + ", prefix: " + header.getName().getPrefix());
                if (header.getName().getLocalPart().contains("Security")) {
                    LOG.info("Adjusting Security tag.");
                    String namespaceURI = header.getName().getNamespaceURI();
                    header.setName(new QName(namespaceURI, "Security", "wsse"));
                }
            }

            for (Header header : message.getHeaders()) {
                LOG.info("Modified/Verified header: " + header.getName().getLocalPart() + ", prefix: " + header.getName().getPrefix());
            }
        }
    }
}