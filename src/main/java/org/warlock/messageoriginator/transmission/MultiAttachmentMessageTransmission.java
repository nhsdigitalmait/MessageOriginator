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
import org.warlock.messageoriginator.MultiAttachmentMessageComponentDescriptor;
import java.util.HashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;
/**
 *
 * @author murff
 */
public class MultiAttachmentMessageTransmission 
    extends Transmission
{
    private String hl7AttachmentId = null;
    private String outputDirectory = null;
    private static final long MIMEHEADERLENGTH = 126L;
    private static final long OTHERLENGTHS = 302L;
    private static final int BUFFERSIZE = 20480;
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
    
    private String saveFile = null;
    
    /** Creates a new instance of MessageTransmission */
    MultiAttachmentMessageTransmission(Wrapper w) {
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


    public String getSaveFile() { return saveFile; }
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
    
    public void buildComplete(int hl7length)
        throws MessageOriginatorException
    {
        // TODO: Complete serialisation to a file. Save the file name
        // set the overall content length when we have the 
        // file. Make the ebXML header first, then calculate the content-length,
        // and write the HTTP header and ebXML MIME part to the file, then the
        // HL7, then iterate through the attachments
        String sendFile = null;
        buildSOAPHeader();
        long contentLength = getContentLength(hl7length);
        StringBuilder s = new StringBuilder();
        getPreHL7HTTPStreamContent(s, contentLength);
        sendFile = wrapper.getHL7().getInteractionId() + "_" + this.getMessageId();
        // saveFile =  sendFile + ".tmp";
        saveFile =  sendFile + ".send";
        outputDirectory = System.getProperty("messageoriginator.destination");
        try {
            File f = new File(outputDirectory, saveFile);
            FileWriter fw = new FileWriter(f);
            fw.write(s.toString());
            fw.write(wrapper.getHL7().getContent());
            MultiAttachmentMessageComponentDescriptor[] m = wrapper.getHL7().getAttachments();
            for (MultiAttachmentMessageComponentDescriptor a : m) {
                a.write(fw);
            }
            fw.write("\r\n----=_MIME-Boundary--");
            fw.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Problem writing multi-attachment transmission file " + saveFile + ": " + e.getMessage());
        }
        // makeSendFile(outputDirectory, sendFile, saveFile);
    }

    private void makeSendFile(String outputDirectory, String sendFile, String saveFile) 
        throws MessageOriginatorException
    {
        String fileName = sendFile + ".send";
        File f = new File(outputDirectory, saveFile);
        long contentLength = f.length();

        StringBuilder s = new StringBuilder();
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
        s.append(Long.toString(contentLength));
        s.append("\r\nContent-Type: ");
        s.append("multipart/related; boundary=\"--=_MIME-Boundary\"; type=\"text/xml\"; start=\"<ebXMLHeader@spine.nhs.uk>\"\r\n");
        s.append("Connection: close\r\n");
        s.append("\r\n");      

        try {
            FileWriter fw = new FileWriter(new File(outputDirectory, fileName));
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(s.toString());
            bw.flush();
            FileReader fr = new FileReader(f);
            char buf[] = new char[BUFFERSIZE];
            int i = 0;
            while ((i = fr.read(buf)) != -1) {
                fw.write(buf, 0, i);
            }
            fw.close();
            fr.close();
            f.delete();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Error making sned file " + fileName + ": " + e.getMessage());
        }
    }
    
    @Override
    String makeBody(String hl7) {
        return null;
    }
    
    @Override
    String makeHeader(int contentLength) {
        return null;
    }
    
    private long getContentLength(long hl7length) {
        // The content length is the size of the soap header, plus the size of the hl7,
        // plus the sum of the sizes of the other attachments, plus the number of attachments
        // including the HL7 times the size of the MIME header, plus the number of attachments
        // plus 3 (for the hl7 and the ebxml and the last one) times the size of the mime boundary
        // plus two for the trailing --, including line breaks.
        //
        // \r\n----=_MIME-Boundary\r\n length = 23, 
        // Content-Id: <ad12e7f6-9464-11dc-985d-39ab3e43f303>\r\n length = 52
        // Content-Type: {type}\r\n length = 16 + length of type string
        // Content-Transfer-Encoding: 8bit\r\n\r\n length = 35
        // total = 126 + length of type string
        
        long l = 0L;
//        System.err.println("SOAPheader length: " + soapHeader.length());
//        l += (long)soapHeader.length();
//        System.err.println("HL7 length: " + hl7length);
//        l += (long)hl7length;
        MultiAttachmentMessageComponentDescriptor[] m = wrapper.getHL7().getAttachments();
        for (MultiAttachmentMessageComponentDescriptor a : m) {
            l += a.getContentLenght();            
        }
//        l += OTHERLENGTHS; // Trailing MIME boundary length with -- rather than \r\n at the end, plus the 
                            // MIME headers for the ebXML and the HL7, and their MIME boundaries
        
        return l;
    }
   
    String getPostHL7HTTPStreamContent() { return POSTHL7; }
    
    void getPreHL7HTTPStreamContent(StringBuilder s, long l)
        throws MessageOriginatorException
    {
        long lng = l + (long)PREHL71.length() + (long)PREHL72.length() + (long)PREHL73.length() +
                   (long)soapHeader.length() + (long)hl7AttachmentId.length() + 
                    (long)POSTHL7.length() + wrapper.getHL7().getLength();
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
        s.append(Long.toString(lng));
        s.append("\r\nContent-Type: ");
        s.append("multipart/related; boundary=\"--=_MIME-Boundary\"; type=\"text/xml\"; start=\"<ebXMLHeader@spine.nhs.uk>\"\r\n");
        s.append("Connection: close\r\n");
        s.append("\r\n");      
        
         s.append(PREHL71);
         s.append(soapHeader);
         s.append(PREHL72);
         s.append(hl7AttachmentId);
         s.append(PREHL73);
        
    }

    
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
         this.substitute(sb, "__OTHERATTACHMENTS__", this.getManifest());
         soapHeader = sb.toString();
    }
    
    private String getManifest() {
        MultiAttachmentMessageComponentDescriptor[] m = wrapper.getHL7().getAttachments();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m.length; i++) {
            if (m[i].usesLarge()) {
                sb.append("<eb:Reference xlink:href=\"");
                sb.append(m[i].getLargeMessageUrl());
                sb.append("\">\r\n");
                sb.append("<eb:Description xml:lang=\"en\">");
                sb.append("Filename=\"");
                sb.append(m[i].getSourceFile());
                sb.append("\" ContentType=");
                sb.append(m[i].getType());
                sb.append(" Compressed=");
                if (m[i].isCompressed()) {
                    sb.append("Yes");
                } else {
                    sb.append("No");
                }
                sb.append(" LargeAttachment=");
                if (m[i].isLargeSingleAttachment()) {
                    sb.append("Yes");
                } else {
                    sb.append("No");
                }
                sb.append(" OriginalBase64=");
                if (m[i].isExternalBase64()) {
                    sb.append("Yes");
                } else {
                    sb.append("No");
                }
                sb.append(" Length=");
                sb.append(m[i].getAttachmentSize());
                if (m[i].getDomainSpecificData() != null) {
                    sb.append(" DomainData=\"");
                    sb.append(m[i].getDomainSpecificData());
                    sb.append("\"");
                }
                sb.append("</eb:Description>\r\n");
                sb.append("</eb:Reference>\r\n");
            } else {
                String cid = this.getUUID();
                m[i].setContentId(cid);
                sb.append("<eb:Reference xlink:href=\"cid:");
                sb.append(cid);
                sb.append("\">\r\n");
                sb.append("<eb:Description xml:lang=\"en\">");
                sb.append(m[i].getSourceFile());
                sb.append("</eb:Description>\r\n");
                sb.append("</eb:Reference>\r\n");
            }
        }
        return sb.toString();
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
