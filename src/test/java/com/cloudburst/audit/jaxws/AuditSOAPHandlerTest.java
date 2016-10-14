package com.cloudburst.audit.jaxws;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ajchesney on 19/08/2016.
 */
public class AuditSOAPHandlerTest {

    @Test
    public void testObscure () {
        AuditSOAPHandler handler = new AuditSOAPHandler().withTextObscured("PASSWORD");
        String str = handler.obscure("foo PASSWORD foo");
        Assert.assertEquals("foo ** obscured ** foo",str);
    }

    @Test
    public void testElementObscure () {
        AuditSOAPHandler handler = new AuditSOAPHandler().withElementsObscured("Password");
        String str = handler.obscure("<logonRequestData>\n" +
                "    <ns2:DomainCode>EXT</ns2:DomainCode>\n" +
                "    <ns2:AgentName>MULAPI</ns2:AgentName>\n" +
                "    <ns2:Password>PASSWORD</ns2:Password>\n" +
                "   </logonRequestData>");
        Assert.assertEquals("<logonRequestData>\n" +
                "    <ns2:DomainCode>EXT</ns2:DomainCode>\n" +
                "    <ns2:AgentName>MULAPI</ns2:AgentName>\n" +
                "    <ns2:Password>** obscured **</ns2:Password>\n" +
                "   </logonRequestData>",str);
    }
}
