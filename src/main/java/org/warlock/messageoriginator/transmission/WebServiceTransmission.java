/*
 * WebServiceTransmission.java
 *
 * Created on 15 March 2006, 23:12
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.transmission;
import org.warlock.messageoriginator.MessageOriginatorException;
import java.util.HashMap;
/**
 *
 * @author murff
 */
public class WebServiceTransmission 
    extends Transmission
{
    
    private static final String POSTHL7 = "</SOAP-ENV:Body></SOAP-ENV:Envelope>\r\n\r\n";
    private String service = null;
    private String action = null;
    private String soapaction = null;
    private String toEndPoint = null;
    private String fromEndPoint = null;
    
    /** Creates a new instance of WebServiceTransmission */
    WebServiceTransmission(Wrapper w) {
        super(w);
    }
    public String getSaveFile() { return null; }
    public String getToEndPoint() { return toEndPoint; }

    void resolveHeaderData()
        throws MessageOriginatorException
    {
        HashMap<String,String> to = null;
        HashMap<String,String> from = null;
        
        to = whelp.getVirtualSDSEntry(wrapper.getHL7().getToAsid(), wrapper.getHL7().getInteractionId(), true);
        if (to == null)
            throw new MessageOriginatorException("Cannot resolve SDS data for to ASID " + wrapper.getHL7().getToAsid());
        from = whelp.getVirtualSDSEntry(wrapper.getHL7().getFromAsid(), wrapper.getHL7().getInteractionId(), false);
        if (from == null)
            throw new MessageOriginatorException("Cannot resolve SDS data for from ASID " + wrapper.getHL7().getFromAsid());

        fromEndPoint = from.get("endpoint");
        from = null;
        soapaction = to.get("soapaction");
        service = to.get("service");
        action = to.get("interaction");
        toEndPoint = to.get("endpoint");
        to = null;
       
    }
    
    public String getAction() { return action; }
    public void buildComplete(int l)
        throws MessageOriginatorException
    {
        throw new MessageOriginatorException("Transmission.buildComplete() should not be called for web services - something has gone horribly wrong");
    }
    void buildSOAPHeader()
        throws MessageOriginatorException
    {
        StringBuilder sb = whelp.getWebServiceHeaderTemplate();
        this.substitute(sb, "__MESSAGE_ID__", this.getMessageId());
        this.substitute(sb, "__ACTION__", soapaction);
        this.substitute(sb, "__RECEIVER_END_POINT__", toEndPoint);
        this.substitute(sb, "__SENDER_END_POINT__", fromEndPoint);
        this.substitute(sb, "__RECEIVER_ASID__", wrapper.getHL7().getToAsid());
        this.substitute(sb, "__SENDER_ASID__", wrapper.getHL7().getFromAsid());
        this.substitute(sb, "__REPLYTO_ADDRESS__", fromEndPoint);
        soapHeader = sb.toString();
    }
    
    /**
     * Make the HTTP stream for this message. The caller is responsible for making and
     * disposing of the buffer.
     */
    void getPreHL7HTTPStreamContent(StringBuilder s, long contentLength)
        throws MessageOriginatorException
    {
         long length = contentLength + POSTHL7.length() + soapHeader.length();
         s.append("POST ");
         s.append(this.getPath());
         s.append(" HTTP/1.1\r\n");
         s.append("Host: ");
         s.append(this.getHost());
         s.append("\r\n");
         s.append("SOAPAction: ");
         s.append(service);
         s.append("/");
         s.append(action);
         s.append("\r\n");
         s.append("Content-Length: ");
         s.append(Long.toString(length));
        // s.append("\r\nContent-Type: application/soap+xml; charset=utf-8\r\n");
         s.append("\r\nContent-Type: text/xml; charset=utf-8\r\n");
         s.append("Content-Transfer-Encoding: 8bit\r\n");
         s.append("Connection: close\r\n\r\n");
         s.append(soapHeader);         
    }
    
    @Override
    String makeBody(String hl7) {
        StringBuilder sb = new StringBuilder();
        sb.append(soapHeader);
        sb.append(hl7);
        sb.append(POSTHL7);
        return sb.toString();
    }
    
    @Override
    String makeHeader(int contentLength) 
            throws MessageOriginatorException
    {
        int cl = contentLength + soapHeader.length() + POSTHL7.length();
        StringBuilder sb = new StringBuilder();
         sb.append("POST ");
         sb.append(this.getPath());
         sb.append(" HTTP/1.1\r\n");
         sb.append("Host: ");
         sb.append(this.getHost());
         sb.append("\r\n");
         sb.append("SOAPAction: ");
         sb.append(service);
         sb.append("/");
         sb.append(action);
         sb.append("\r\n");
         sb.append("Content-Length: ");
         sb.append(Integer.toString(cl));
        // s.append("\r\nContent-Type: application/soap+xml; charset=utf-8\r\n");
         sb.append("\r\nContent-Type: text/xml; charset=utf-8\r\n");
         sb.append("Content-Transfer-Encoding: 8bit\r\n");
         sb.append("Connection: close\r\n\r\n");
        
        return sb.toString();
    }
    
    String getPostHL7HTTPStreamContent() {
        return POSTHL7;
    }
}
