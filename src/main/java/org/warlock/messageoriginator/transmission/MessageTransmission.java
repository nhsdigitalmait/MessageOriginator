/*
 * MessageTransmission.java
 *
 * Created on 15 March 2006, 23:11
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
public class MessageTransmission 
    extends Transmission
{
    private String hl7AttachmentId = null;
    private static final String POSTHL7 = "\r\n----=_MIME-Boundary--";
    private static final String PREHL71 = "----=_MIME-Boundary\r\nContent-Id: <ebXMLHeader@spine.nhs.uk>\r\nContent-Type: text/xml; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n";
    private static final String PREHL72 = "\r\n----=_MIME-Boundary\r\nContent-Id: <";
    //private static final String PREHL73 = ">\r\nContent-Type: application/XML; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n";
    private static final String PREHL73 = ">\r\nContent-Type: application/xml; charset=UTF-8\r\nContent-Transfer-Encoding: 8bit\r\n\r\n";
    
    private String toPartyId = null;
    private String fromPartyId = null;
    private String cpaId = null;
    private String mhsActor = null;
    private String syncReply = null;
    private String service = null;
    private String action = null;
    private String serviceAction = null;
    private String duplicateElimination = null;
    private String ackRequested = null;
    private String toEndPoint = null;
    private String fromEndPoint = null;
    
    /** Creates a new instance of MessageTransmission */
    MessageTransmission(Wrapper w) {
        super(w);
        makeAttachmentId();
    }
    public String getToPartyId() { return toPartyId; }
    public String getFromPartyId() { return fromPartyId; }
    public String getCPAid() { return cpaId; }
    public String getMHSActor() { return mhsActor; }
    public String getSyncReply() { return syncReply; }
    public String getService() { return service; }
    public String getAction() { return action; }
    public String getServiceAction() { return serviceAction; }
    public String getDuplicateElimination() { return duplicateElimination; }
    public String getAckRequested() { return ackRequested; }
    public String getToEndPoint() { return toEndPoint; }
    public String getFromEndPoint() { return fromEndPoint; }


   void resolveHeaderData()
        throws MessageOriginatorException
    {
        HashMap<String,String> to = null;
        HashMap<String,String> from = null;
        
        to = whelp.getSDSInteractionEntry(wrapper.getHL7().getToAsid(), wrapper.getHL7().getInteractionId());
        if (to == null)
            throw new MessageOriginatorException("Cannot resolve SDS data for to ASID " + wrapper.getHL7().getToAsid());
        from = whelp.getSDSInteractionEntry(wrapper.getHL7().getFromAsid(), null);
        if (from == null)
            throw new MessageOriginatorException("Cannot resolve SDS data for from ASID " + wrapper.getHL7().getFromAsid());

        fromPartyId = from.get("partykey");
        fromEndPoint = from.get("endpoint");
        from = null;
        toPartyId = to.get("partykey");
        cpaId = to.get("cpaid");
        mhsActor = to.get("mhsactor");
        syncReply = to.get("syncReply");
        service = to.get("service");
        action = to.get("interaction");
        serviceAction = to.get("soapaction");
        duplicateElimination = to.get("dupElim");
        ackRequested = to.get("ackRq");
        toEndPoint = to.get("endpoint");
        to = null;
    }
    
    
    String getPostHL7HTTPStreamContent() { return POSTHL7; }
    
    public void buildComplete(int l)
        throws MessageOriginatorException
    {
        throw new MessageOriginatorException("Transmission.buildComplete() should not be called here - something has gone horribly wrong");
    }
    
    @Override
    String makeBody(String hl7) {
        StringBuilder sb = new StringBuilder();

         sb.append(PREHL71);
         sb.append(soapHeader);
         sb.append(PREHL72);
         sb.append(hl7AttachmentId);
         sb.append(PREHL73);
         sb.append(hl7);
         sb.append(POSTHL7);
        return sb.toString();
    }
    
    @Override
    String makeHeader(int hl7length)
            throws MessageOriginatorException
    {
        int cl = PREHL71.length() + soapHeader.length() + PREHL72.length() +
                hl7AttachmentId.length() + hl7length + PREHL73.length() +POSTHL7.length();
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
        sb.append("\r\nContent-Type: ");
        sb.append("multipart/related; boundary=\"--=_MIME-Boundary\"; type=\"text/xml\"; start=\"<ebXMLHeader@spine.nhs.uk>\"\r\n");
        sb.append("Connection: close\r\n");
        sb.append("\r\n");
        return sb.toString();
    }
    
    void getPreHL7HTTPStreamContent(StringBuilder s, long contentLength)
        throws MessageOriginatorException
    {
         long length = contentLength + hl7AttachmentId.length() 
                        + POSTHL7.length() + PREHL71.length() + PREHL72.length() 
                        + PREHL73.length() + soapHeader.length();
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
         s.append("\r\nContent-Type: ");
         s.append("multipart/related; boundary=\"--=_MIME-Boundary\"; type=\"text/xml\"; start=\"<ebXMLHeader@spine.nhs.uk>\"\r\n");
         // This might be a bug - MIME uses Content-Transfer-Encoding, HTTP uses Transfer-Encoding
         //s.append("Content-Transfer-Encoding: 8bit\r\n\r\n");
         //s.append("Transfer-Encoding: 8bit\r\n\r\n");
         s.append("Connection: close\r\n");
         s.append("\r\n");
         s.append(PREHL71);
         s.append(soapHeader);
         s.append(PREHL72);
         s.append(hl7AttachmentId);
         s.append(PREHL73);
         
    }

    public String getSaveFile() { return null; }
    void buildSOAPHeader()
        throws MessageOriginatorException
    {
        /*
         * We've no real interest in the SOAP structure as a formal XML document - just in the
         * serialisation. So make the SOAP envelope in a StringBuilder. This is loaded from a
         * template file (that WrapperHelper knows about), and then constructed by substitution.
         */
         StringBuilder sb = whelp.getMessageHeaderTemplate();
         this.substitute(sb, "__TOPARTYID__", toPartyId);
         this.substitute(sb, "__FROMPARTYID__", fromPartyId);
         this.substitute(sb, "__CPAID__", cpaId);
         /*
          * DJM: 20071011 fixed this to use the same UUID for the conversation id as we're
          * using for the message id (i.e. call this.getMessageId()) 
          */
         this.substitute(sb, "__CONVERSATIONID__", this.getMessageId());
         this.substitute(sb, "__SERVICENAME__", service);
         this.substitute(sb, "__ACTIONNAME__", action);
         this.substitute(sb, "__MESSAGEID__", this.getMessageId());
         this.substitute(sb, "__TIMESTAMP__", this.getISO8601TimeStamp());
         if (getDuplicateElimination().equals("always")) {
             this.substitute(sb, "__DUPLICATEELIMINATION__", "<eb:DuplicateElimination/>");
         } else {
             this.substitute(sb, "__DUPLICATEELIMINATION__", "");
         }
         if (getAckRequested().equals("always")) {
             StringBuilder ar = new StringBuilder("<eb:AckRequested SOAP:mustUnderstand=\"1\" eb:version=\"2.0\" ");
             ar.append("eb:signed=\"false\" SOAP:actor=\"");
             ar.append(mhsActor);
             ar.append("\"/>");
             this.substitute(sb, "__ACKREQUESTED__", ar.toString());
         } else {
             this.substitute(sb, "__ACKREQUESTED__", "");
         }
         if (!getSyncReply().equals("None")) {
             this.substitute(sb, "__SYNCREPLY__", "<eb:SyncReply SOAP:mustUnderstand=\"1\" eb:version=\"2.0\" SOAP:actor=\"http://schemas.xmlsoap.org/soap/actor/next\"/>");
         } else {
             this.substitute(sb, "__SYNCREPLY__", "");
         }
         this.substitute(sb, "__HL7XLINKREF__", this.getHL7XlinkRef());
         this.substitute(sb, "__OTHERATTACHMENTS__", "");
         soapHeader = sb.toString();
    }
    
    private String getHL7XlinkRef() {
        StringBuilder sb = new StringBuilder("cid:");
        sb.append(hl7AttachmentId);
        return sb.toString();
    }
    
    private void makeAttachmentId() {
        StringBuilder sb = new StringBuilder(this.getUUID());
        sb.append("@spine.nhs.uk");
        hl7AttachmentId = sb.toString();
    }
}
