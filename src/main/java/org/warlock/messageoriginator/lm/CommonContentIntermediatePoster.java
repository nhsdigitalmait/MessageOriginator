/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.lm;
import org.safehaus.uuid.UUIDGenerator;
import org.safehaus.uuid.UUID;
import org.warlock.messageoriginator.MultiAttachmentMessageComponentDescriptor;
import org.warlock.messageoriginator.MessageOriginatorException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.warlock.messageoriginator.hl7.HL7Interaction;

/**
 *
 * @author DAMU2
 */
public class CommonContentIntermediatePoster 
    implements LMIntermediatePoster
{
    private static String commonContentTemplate = null;
    private static String attachmentDirectoryName = null;
    private static File attachmentDirectory = null; 
    private static String commonContentDirectoryName = null;
    private static File commonContentDirectory = null;
    private static String attachmentDescriptorDirectoryName = null;
    private static File attachmentDescriptorDirectory = null;

    private static String lsaSplitDirectoryName = null;
    private static File lsaSplitDirectory = null;
    private static String lsaSplitDescriptorDirectoryName = null;
    private static File lsaSplitDescriptorDirectory = null;
    private static String lsaSplitAttachmentDirectoryName = null;
    private static File lsaSplitAttachmentDirectory = null;

    private static final SimpleDateFormat ROOTFORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    
    private static final String COMMONCONTENTTEMPLATEPROPERTY = "messageoriginator.lm.commoncontenttemplate";
    private static final String ATTACHMENTDIRECTORYPROPERTY = "messageoriginator.lm.attachmentdirectory";
    private static final String CCHL7DIRECTORYNAMEPROPERTY = "messageoriginator.lm.commoncontenthl7directory";
    private static final String DESCRIPTORDIRECTORYPROPERTY = "messageoriginator.lm.commoncontentdescriptordirectory";
    private static final String LSASPLITDIRECTORYPROPERTY = "messageoriginator.lm.lsasplitdirectory";
    private static final String LSASPLITDESCRIPTORDIRECTORYPROPERTY = "messageoriginator.lm.lsasplitdescriptordirectory";
    private static final String LSASPLITATTACHMENTDIRECTORYPROPERTY = "messageoriginator.lm.lsasplitattachmentdirectory";

    private static final String SPLITSIZEPROPERTY = "messageoriginator.lm.lsasplitsize";

    private static final int BUFFER = 1024;
    private static final int DEFAULTSPLITSIZE = 1024000; // 1024 kilobytes - sort of
    private static int splitSize = 0;
        
    public CommonContentIntermediatePoster()
            throws MessageOriginatorException
    {
        if (commonContentTemplate == null) {
            init();
        } 
    }
    
    private void init()
            throws MessageOriginatorException
    {
        if (System.getProperty(COMMONCONTENTTEMPLATEPROPERTY) == null) {
            throw new MessageOriginatorException("Failed to initialise: " + COMMONCONTENTTEMPLATEPROPERTY + " property missing");
        }
        attachmentDirectoryName = System.getProperty(ATTACHMENTDIRECTORYPROPERTY);
        commonContentDirectoryName = System.getProperty(CCHL7DIRECTORYNAMEPROPERTY);
        attachmentDescriptorDirectoryName = System.getProperty(DESCRIPTORDIRECTORYPROPERTY);
        lsaSplitDirectoryName = System.getProperty(LSASPLITDIRECTORYPROPERTY);
        lsaSplitDescriptorDirectoryName = System.getProperty(LSASPLITDESCRIPTORDIRECTORYPROPERTY);
        lsaSplitAttachmentDirectoryName = System.getProperty(LSASPLITATTACHMENTDIRECTORYPROPERTY);

        attachmentDirectory = initDirectory(ATTACHMENTDIRECTORYPROPERTY, attachmentDirectoryName);
        commonContentDirectory = initDirectory(CCHL7DIRECTORYNAMEPROPERTY, commonContentDirectoryName);
        attachmentDescriptorDirectory = initDirectory(DESCRIPTORDIRECTORYPROPERTY, attachmentDescriptorDirectoryName);
        lsaSplitDirectory = initDirectory(LSASPLITDIRECTORYPROPERTY, lsaSplitDirectoryName);
        lsaSplitDescriptorDirectory = initDirectory(LSASPLITDESCRIPTORDIRECTORYPROPERTY, lsaSplitDescriptorDirectoryName);
        lsaSplitAttachmentDirectory = initDirectory(LSASPLITATTACHMENTDIRECTORYPROPERTY, lsaSplitAttachmentDirectoryName);

        loadCommonContentTemplate(System.getProperty(COMMONCONTENTTEMPLATEPROPERTY));

        if (System.getProperty(SPLITSIZEPROPERTY) == null) {
            splitSize = DEFAULTSPLITSIZE;
        }
        try {
            splitSize = Integer.parseInt(System.getProperty(SPLITSIZEPROPERTY));
        }
        catch (Exception e) {
            splitSize = DEFAULTSPLITSIZE;
        }
    }
    
    private File initDirectory(String property, String name)
            throws MessageOriginatorException
    {
        if (name == null) {
            throw new MessageOriginatorException("Failed to initialise: " + property + " property not set");
        }
        File f = null;
        try {
            f = new File(name);
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to initialise: " + name + " from properties cannot be made into a File: " + e.getMessage());
        }
        if (!f.isDirectory()) {
            throw new MessageOriginatorException("Failed to intialise: " + name + " from properties is not a directory");
        }
        return f;
    }
    private void loadCommonContentTemplate(String tname)
            throws MessageOriginatorException
    {
        try {
            File f = new File(tname);
            BufferedReader br = new BufferedReader(new FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\r\n");
            }
            br.close();
            commonContentTemplate = sb.toString();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to initialise common content template: " + e.getMessage());
        }
    }
    
    public String post(InputStream r, MultiAttachmentMessageComponentDescriptor d) 
            throws MessageOriginatorException
    {
        String fragmentId = UUIDGenerator.getInstance().generateRandomBasedUUID().toString().toUpperCase();
        if (d.isLargeSingleAttachment()) {
            doLargeAttachmentSequence(fragmentId, r, d);
        } else {
            writeStream(r, fragmentId, -1, false);
            String ccFileName = makeCommonContentMessage(fragmentId, d, false);
            makeDescriptorFile(fragmentId, d, ccFileName, false);
        }
        return "mid:" + fragmentId;
    }

    private void doLargeAttachmentSequence(String fragmentId, InputStream r, MultiAttachmentMessageComponentDescriptor d)
        throws MessageOriginatorException
    {
        d.setSplitCount(doSplit(fragmentId, r));
        String ccFileName = makeCommonContentMessage(fragmentId, d, true);
        makeDescriptorFile(fragmentId, d, ccFileName, true);
    }

    private int doSplit(String fragmentId, InputStream r)
            throws MessageOriginatorException
    {
        int partNumber = 0;
        byte buffer[] = new byte[splitSize];
        int chunkSize = 0;

        try {
            java.util.Arrays.fill(buffer, (byte)0);
            while ((chunkSize = r.read(buffer, 0, splitSize)) != -1) {
                ByteArrayInputStream bais = new ByteArrayInputStream(buffer, 0, chunkSize);
                writeStream(bais, fragmentId, partNumber, true);
                partNumber++;
                java.util.Arrays.fill(buffer, (byte)0);
            }
            r.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Split : " + partNumber + " : Cannot save stream: " + e.getMessage());
        }
        return partNumber;
    }

    private void makeDescriptorFile(String fragmentId, MultiAttachmentMessageComponentDescriptor d, String ccFileName, boolean lsa)
            throws MessageOriginatorException
    {
        StringBuilder specFile = null;
        if (lsa) {
            specFile = new StringBuilder(lsaSplitDescriptorDirectoryName);
        } else {
            specFile = new StringBuilder(attachmentDescriptorDirectoryName);
        }
        specFile.append(System.getProperty("file.separator"));
        specFile.append(fragmentId);
        specFile.append(".spec");
        String filename = specFile.toString();
        
        specFile = new StringBuilder("HL7part\t");
        specFile.append("LM_CommonContent_");
        specFile.append(ccFileName);
        specFile.append("\n");
        if (lsa) {
            for (int i = 0; i < d.getSplitCount(); i++) {
                specFile.append(fragmentId);
                specFile.append("_");
                specFile.append(i);
                specFile.append(".messageattachment");
                specFile.append("\t");
                specFile.append(d.getType());
                // "No" to base64, "Yes" to usesLarge, "No" to compressed and large single attachment
                // - see if it is already base64, also no domain specific data
                specFile.append("\tn\ty\tn\tn\t");
                if (d.isExternalBase64()) {
                    specFile.append("y");
                } else {
                    specFile.append("n");
                }
                specFile.append("\n");
            }
        } else {
            specFile.append(fragmentId);
            specFile.append(".messageattachment");
            specFile.append("\t");
            specFile.append(d.getType());
            // "No" to base64, usesLarge, compressed and large single attachment
            // check for external base64, also no domain specific data
            specFile.append("\tn\tn\tn\tn\t");
            if (d.isExternalBase64()) {
                specFile.append("y");
            } else {
                specFile.append("n");
            }
            specFile.append("\n");
        }
        String spec = specFile.toString();
        
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(spec.toString());
            fw.flush();
            fw.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to write spec file " + filename + " : " + e.getMessage());
        }
        
    }
    
    private String makeCommonContentMessage(String fragmentId, MultiAttachmentMessageComponentDescriptor d, boolean lsa)
            throws MessageOriginatorException
    {
        StringBuilder sb = new StringBuilder(commonContentTemplate);
        String timestamp = ROOTFORMAT.format(new Date());
        substitute(sb, "__CREATIONTIMESTAMP__", timestamp);
        substitute(sb, "__MESSAGEID__", fragmentId);
        substitute(sb, "__FROMASID__", d.getHL7Interaction().getFromAsid());
        substitute(sb, "__TOASID__", d.getHL7Interaction().getToAsid());
        substitute(sb, "__MAIN_MESSAGE_ID__", d.getHL7Interaction().getMessageId());
        substitute(sb, "__ATTACHMENT_FILENAME__", d.getSourceFile());
        
        StringBuilder ccFile = null;
        if (lsa) {
            ccFile = new StringBuilder(lsaSplitDirectoryName);
        } else {
            ccFile = new StringBuilder(commonContentDirectoryName);
        }

        ccFile.append(System.getProperty("file.separator"));
        ccFile.append("LM_CommonContent_");
        ccFile.append(fragmentId);
        ccFile.append(".xml");
        String filename = ccFile.toString();
        
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(sb.toString());
            fw.flush();
            fw.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to write common content message " + filename + " : " + e.getMessage());
        }
        return fragmentId + ".xml";
    }

    private void substitute(StringBuilder sb, String tag, String content)
        throws MessageOriginatorException
    {
        int tagStart = sb.indexOf(tag);
        int tagEnd = -1;
        if (tagStart == -1) 
            throw new MessageOriginatorException("Invalid sustitution tag: " + tag + " - not found in Common Content template");
        do {
            tagEnd = tagStart + tag.length();
            sb.replace(tagStart, tagEnd, content);
            tagStart = sb.indexOf(tag, tagEnd);
        } while (tagStart != -1);
    }    
    
    
    private void writeStream(InputStream r, String fragmentId, int part, boolean lsa)
            throws MessageOriginatorException  
    {
        StringBuilder sbf = null;
        if (lsa) {
            sbf = new StringBuilder(lsaSplitAttachmentDirectoryName);
        } else {
            sbf = new StringBuilder(attachmentDirectoryName);
        }
        sbf.append(System.getProperty("file.separator"));
        sbf.append(fragmentId);
        if (part != -1) {
            sbf.append("_");
            sbf.append(part);
        }
        sbf.append(".messageattachment");
        String filename = sbf.toString();
        try {
            FileOutputStream fOut = new FileOutputStream(filename);
            byte buffer[] = new byte[BUFFER];
            java.util.Arrays.fill(buffer, (byte)0);
            int in = 0;            
            while ((in = r.read(buffer)) != -1) {
                fOut.write(buffer, 0, in);
                java.util.Arrays.fill(buffer, (byte)0);
            }
            r.close();
            fOut.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Cannot save stream: " + e.getMessage());
        }
    }
    
}
