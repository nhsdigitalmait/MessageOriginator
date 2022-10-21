/*
 * Wrapper.java
 *
 * Created on 17 March 2006, 11:07
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.transmission;
import org.warlock.messageoriginator.MessageOriginatorException;
import org.warlock.messageoriginator.hl7.HL7Interaction;
import java.util.HashMap;

/**
 *
 * @author murff
 */
public class Wrapper {
    
    private HL7Interaction hl7;
    private WrapperHelper whelp = null;
    
    private Transmission transmission = null;
    
    /** Creates a new instance of Wrapper */
    public Wrapper(HL7Interaction h) {
        hl7 = h;
        whelp = WrapperHelper.getInstance();
    }
    
    public HL7Interaction getHL7() { return hl7; }
    
    
    public synchronized void makeTransmission() 
        throws MessageOriginatorException
    {
        if (transmission != null) {
            return;
        }
        if (isMessage()) {
            if (hl7.hasOtherAttachments()) {
                transmission = new MultiAttachmentMessageTransmission(this);
                transmission.resolveHeaderData();
            } else {
                transmission = new MessageTransmission(this);
                transmission.resolveHeaderData();
                transmission.buildSOAPHeader();        
            }
        } else {
            transmission = new WebServiceTransmission(this);
            transmission.resolveHeaderData();
            transmission.buildSOAPHeader();        
        }
    }
    
    public String serialiseTransmission()
        throws MessageOriginatorException
    {
        makeTransmission();
        if (hl7.hasOtherAttachments()) {
            transmission.buildComplete((int)hl7.getLength());
            return null;
        } else {
            String hl7data = hl7.getContent();
           // char[] buffer = hl7.loadFile();
            StringBuilder sb = new StringBuilder();
//            transmission.getPreHL7HTTPStreamContent(sb, hl7.getLength());
//            sb.append(hl7data);
//            sb.append(transmission.getPostHL7HTTPStreamContent());
            
            // TODO: This needs fixing. It breaks when there are Unicode characters
            // (e.g. that daft delimiter in GPES messages). Needs to be based on the
            // actual file length in bytes, not string length in Java characters.
            
            String body = transmission.makeBody(hl7data);
            int cl = (int)hl7.getLength();
            String hdr = transmission.makeHeader(cl);
            sb.append(hdr);
            sb.append(body);
            return sb.toString();
        }
    }
 
    public String getAction() { return transmission.getAction(); }
    
    public Transmission getTransmission()
        throws MessageOriginatorException
    {
        if (transmission == null)
            makeTransmission();
        return transmission;
    }
    
    private boolean isMessage() 
        throws MessageOriginatorException
    {
        String itype = whelp.resolveInteractionType(hl7.getInteractionId());
        return itype.equals("MESSAGE");
    }

    public String getTransmissionId() {
        return transmission.getMessageId();
    }
}
