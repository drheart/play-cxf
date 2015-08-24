package org.apache.cxf.transport.play;

import java.util.List;
import java.util.logging.Logger;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.interceptor.*;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

/**
 * This class is critical to supporting WS-Security extensions in a manner compatible with the official WSS4J library.
 * There is some degree of ambiguity in the specification for WS-Security such that an implementation may use an alternative
 * namespace name for the Security tag (other than "wsse"), such that the namespace still references the correct schema.
 * WSS4J, however, has the expectation that it is "wsse". We accommodate for this with the following interceptor.
 */
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