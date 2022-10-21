/*
 * HL7Interaction.java
 *
 * Created on 15 March 2006, 20:58
 *
 * The HL7Interaction encapsulates a pre-generated HL7 interaction instance.
 * The interaction is in a file. Most of the content is private to the
 * instance and is not used for "wrapping" with SOAP and ebXML constructs,
 * but the instance type and message id are required and these are read when
 * the instance is verified.
 */

package org.warlock.messageoriginator.hl7;

import java.io.File;
//import java.io.FileReader;
import java.io.FileInputStream;
//import java.io.CharArrayReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
//import java.io.IOException;
//import org.warlock.messageoriginator.*;
import org.xml.sax.InputSource;
//import javax.xml.xpath.XPathExpression;
//import javax.xml.xpath.XPathExpressionException;
import org.warlock.messageoriginator.XMLhelper;
import org.warlock.messageoriginator.MessageOriginatorException;
import org.warlock.messageoriginator.MultiAttachmentMessageComponentDescriptor;
/**
 *
 * @author murff
 */
public class HL7Interaction {
    
    // Private members for managing the interaction file
    //
    private String msgFile = null;
    private String fileProblem = null;
    private boolean fileOK = false;
    private long fileSize = 0;
    private boolean otherAttachments = false;
    private long contentLength = 0;
    
    // In case we're making a multi-attachment message
    //
    private MultiAttachmentMessageComponentDescriptor attachments[] = null;
    
    // Private members for HL7 data
    //
    private String interactionid = null;
    private String messageid = null;
    private String toAsid = null;
    private String fromAsid = null;
    
    
    /** Creates a new instance of HL7Interaction with separate file and path names */
    public HL7Interaction(String filename, String path) {
        this.setFile(filename, path);
    }
    
    /** Creates a new instance of HL7Interaction with a single file name
     * @param filename */
    public HL7Interaction(String filename) {
        this.setFile(filename);
    }
    
    /** Creates a new instance of HL7Interaction */
    public HL7Interaction() {}
    
    public void setAttachments(MultiAttachmentMessageComponentDescriptor a[]) {
        attachments = a;
        if ((attachments != null) && (attachments.length > 0)) {
            otherAttachments = true;
        } else {
            otherAttachments = false;
        }
    }
    
    public MultiAttachmentMessageComponentDescriptor[] getAttachments() {
        return attachments;
    }
    
    public void setFile(String filename) {
        msgFile = filename;
    }

    
    
    public void setFile(String filename, String path) {
          StringBuffer sb = new StringBuffer(path);
        //sb.append(System.getProperty("file.separator"));
          sb.append("/");
        sb.append(filename);
        msgFile = sb.toString();      
    }
    public boolean hasOtherAttachments() { return otherAttachments; }
    public String getFileName() { return msgFile; }
    public String getFileProblem() { return fileProblem; }
    
    /** Checks that the file is OK, loads it and extracts information for the
     transmission header builders */
    public void load()
        throws MessageOriginatorException {        
        if (!fileOK && (fileProblem == null)) {
//            char[] buffer = null;
            byte[] buffer = null;
            buffer = this.loadFile();
            this.readContent(buffer);
            fileOK = true;
            buffer = null;
        }
    }
    
    public String getContent()
        throws MessageOriginatorException
    {
//        char[] buffer = this.loadFile();
        byte[] buffer = this.loadFile();
        return new String(buffer);
    }
    
    /** Returns true if the file has been read and its content extracted correctly.
     */
    public boolean isOK() { return fileOK; }
    
   //public long getContentLength() { return contentLength; }
    
    /** Returns length of interaction file in bytes.
     */
    public long getLength() { return fileSize; }
    
    public String getToAsid() { return toAsid; }
    
    public String getFromAsid() { return fromAsid; }
    
    /**
     * Get the message id extracted from the message. This will return null
     * if it is called before load().
     */
    public String getMessageId() { return messageid; }
    
    /**
     * Get the interaction id extracted from the message. This will return null
     * if it is called before load().
     */
    public String getInteractionId() { return interactionid; }
    
    
    private void readContent(byte[] buffer) 
        throws MessageOriginatorException {
        
        try {
            XMLhelper xmlh = XMLhelper.getInstance();
//            CharArrayReader car = new CharArrayReader(buffer);
            InputSource inMsgXml = new InputSource(new ByteArrayInputStream(buffer));
            interactionid = xmlh.getInteractionIdXpath().evaluate(inMsgXml);
//            car = new CharArrayReader(buffer);
            inMsgXml = new InputSource(new ByteArrayInputStream(buffer));
            messageid = xmlh.getMessageIdXpath().evaluate(inMsgXml);
//            car = new CharArrayReader(buffer);
            inMsgXml = new InputSource(new ByteArrayInputStream(buffer));
            toAsid = xmlh.getToAsidXpath().evaluate(inMsgXml);
//            car = new CharArrayReader(buffer);
            inMsgXml = new InputSource(new ByteArrayInputStream(buffer));
            fromAsid = xmlh.getFromAsidXpath().evaluate(inMsgXml);
            inMsgXml = null;
//            car = null;                   
        }
        catch (Exception e) {
            buffer = null;
            fileProblem = e.getMessage();
            throw new MessageOriginatorException(fileProblem);            
        }
    }
    
    public byte[] loadFile()
        throws MessageOriginatorException {
       
        byte[] buffer = null;
        if ((msgFile == null) || (msgFile.length() == 0)) {
                fileProblem = "No file";
                throw new MessageOriginatorException(fileProblem);
        }
        try {
            File f;
            //FileReader fReader;
            FileInputStream fis = null;
            
            f = new File(msgFile);
            fileSize = f.length();
            if (fileSize == 0L) {
                fileProblem = new String("No file " + msgFile);
                throw new MessageOriginatorException(fileProblem);
            }
            buffer = new byte[(int)fileSize];
            //fReader = new FileReader(f);
            fis = new FileInputStream(f);
//            if (fReader.read(buffer, 0, (int)fileSize) == -1) {
            if (fis.read(buffer, 0, (int)fileSize) == -1) {
                buffer = null;
                fileProblem = new String("Premature EOF reading " + msgFile);
                throw new MessageOriginatorException(fileProblem);
            }
//            fReader = null;
            fis.close();
            fis = null;
        }
        catch (Exception e) {
            fileProblem = e.getMessage();
            throw new MessageOriginatorException(fileProblem);
        }
        return buffer;
    }
    
}
