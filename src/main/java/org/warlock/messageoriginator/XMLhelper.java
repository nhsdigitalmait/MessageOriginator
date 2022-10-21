/*
 * XMLhelper.java
 *
 * Created on 17 March 2006, 10:10
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator;

import javax.xml.xpath.*;

/** 
 *
 * @author murff
 *
 * This class exists to house pre-compiled XPathExpression instances and other 
 * XML-ey things for use by the HL7Interaction and the reference data classes.
 */
public class XMLhelper {
    
    private static final String MESSAGEID = "/*[1]/hl7:id/@root";
    private static final String INTERACTIONID = "/*[1]/hl7:interactionId/@extension";
    private static final String TOASID = "/*[1]/hl7:communicationFunctionRcv/hl7:device/hl7:id/@extension";
    private static final String FROMASID = "/*[1]/hl7:communicationFunctionSnd/hl7:device/hl7:id/@extension";

    private XPathExpression messageId = null; 
    private XPathExpression interactionId = null;
    private XPathExpression toAsid = null; 
    private XPathExpression fromAsid = null; 
    
    private static XMLhelper me = new XMLhelper();
    
    private XMLhelper() {
        try {
            // Yes, we're just going to discard any exceptions here because we don't
            // get to deploy the application if anything *is* broken in this helper.
            XPath x;
            XMLHelperNamespaceContext hl7ns = new XMLHelperNamespaceContext();
            hl7ns.declarePrefix("hl7","urn:hl7-org:v3");
            x = XPathFactory.newInstance().newXPath();
            x.setNamespaceContext(hl7ns);
            messageId = x.compile(MESSAGEID);
            x = XPathFactory.newInstance().newXPath();
            x.setNamespaceContext(hl7ns);
            interactionId = x.compile(INTERACTIONID);
            x = XPathFactory.newInstance().newXPath();
            x.setNamespaceContext(hl7ns);
            toAsid = x.compile(TOASID);
            x = XPathFactory.newInstance().newXPath();
            x.setNamespaceContext(hl7ns);
            fromAsid = x.compile(FROMASID);
        }
        catch (Exception e) {}
    }
    
    public synchronized static XMLhelper getInstance() { return me; }
    
    public XPathExpression getMessageIdXpath() { return messageId; }
    
    public XPathExpression getInteractionIdXpath() { return interactionId; }
    
    public XPathExpression getToAsidXpath() { return toAsid; }
    
    public XPathExpression getFromAsidXpath() { return fromAsid; }
    
}
