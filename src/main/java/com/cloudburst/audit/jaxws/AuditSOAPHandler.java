package com.cloudburst.audit.jaxws;

import com.cloudburst.audit.AuditorSingleton;
import com.cloudburst.audit.model.AuditItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

import ch.qos.logback.classic.Level;

/**
 * Audit SOAP Messages if this handler added to the chain
 */
public class AuditSOAPHandler implements SOAPHandler<SOAPMessageContext> {

    private final static Logger logger = LoggerFactory.getLogger(AuditSOAPHandler.class);

    private final ThreadLocal<AuditItem> REQUEST_ITEMS = new ThreadLocal<>();

    @Override
    public boolean handleMessage(SOAPMessageContext ctx) {
        if (!AuditorSingleton.isPopulated()) {
            return true;
        }
        boolean isOutboundMessage=  (Boolean)ctx.get (MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if ( isOutboundMessage ) {
            AuditItem requestItem = createRequestItem(ctx);
            REQUEST_ITEMS.set(requestItem);
            auditItem(requestItem);
        }
        else {
            AuditItem requestItem = REQUEST_ITEMS.get();
            if ( requestItem == null ) {
                logger.warn("No request item bound to thread!");
            }
            else {
                AuditItem responseItem = createResponseItem(requestItem,ctx);
                auditItem(responseItem);
            }
        }
        return true;
    }

    protected AuditItem createRequestItem(SOAPMessageContext ctx) {
        return AuditItem.request(
                url(ctx),
                this.getClass().getSimpleName(),
                label(ctx),
                null,
                messageToString(ctx.getMessage()),
                "application/soap+xml"
        );
    }

    protected AuditItem createResponseItem(AuditItem requestItem, SOAPMessageContext ctx) {
        return AuditItem.response(
                requestItem,
                url(ctx),
                this.getClass().getSimpleName(),
                label(ctx),
                null,
                messageToString(ctx.getMessage()),
                "application/soap+xml"
        );
    }

    private String url(SOAPMessageContext ctx) {
        String result = (String) ctx.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        if ( result == null ) {
            result = "no endpoint addresss available";
        }
        return result;
    }

    /**
     * Use Operation name as label
     * @param ctx
     * @return
     */
    private String label(SOAPMessageContext ctx) {
        QName qname = (QName) ctx.get(MessageContext.WSDL_OPERATION);
        return qname.getLocalPart();
    }

    @Override
    public boolean handleFault(SOAPMessageContext ctx) {
        if (!AuditorSingleton.isPopulated()) {
            return true;
        }
        AuditItem faultItem = createFaultItem(ctx);
        auditItem(faultItem);
        return true;
    }

    protected AuditItem createFaultItem(SOAPMessageContext ctx) {
        return AuditItem.exception(
                Level.WARN.toString(),
                this.getClass().getSimpleName(),
                "SOAP FAULT",
                messageToString(ctx.getMessage())
        );
    }

    protected void auditItem(AuditItem item) {
        if ( item != null && AuditorSingleton.isPopulated() ){
            AuditorSingleton.getInstance().audit(item);
        }
    }

    @Override
    public void close(MessageContext context) {
        REQUEST_ITEMS.remove();
    }

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    private String messageToString(SOAPMessage message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            message.writeTo(baos);
            baos.close();
            return new String(baos.toByteArray(),"UTF-8");
        } catch (SOAPException e) {
            logger.warn("messageToString",e);
        } catch (IOException e) {
            logger.warn("messageToString",e);
        }
        return "exception in messageToString";
    }
}
