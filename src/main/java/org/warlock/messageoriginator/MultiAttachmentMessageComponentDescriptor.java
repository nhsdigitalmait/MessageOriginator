/*
 * MultiAttachmentMessageComponentDescriptor.java
 *
 * Created on 20 November 2007, 10:24
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.util.Arrays;
import org.apache.commons.codec.binary.Base64;
import org.warlock.messageoriginator.lm.LargeMessagePosterSingleton;
import org.warlock.messageoriginator.hl7.HL7Interaction;
/**
 *
 * @author DAMU2
 */
public class MultiAttachmentMessageComponentDescriptor {
 
    private static final String MULTISOURCE = "messageoriginator.source";
    private static final String MULTIWORK = "messageoriginator.workdir";
    private static final String MULTIDATA = "messageoriginator.datadir";
    private static final String B64FILEBUFFER = "messageoriginator.base64buffersize";
    private static final String LIEABOUTMIMECONTENTDESCRIPTION = "messageoriginator.contentdescriptor";
    private static final int DEFAULTB64BUFFER = 30720; 
    private static final int CHUNKSIZE = 64;
    
    private static int base64buffer = 0;
    private static String workDir = null;
    private static String sourceDir = null;
    private static String dataDir = null;
    private String sourceFile = null;
    private String fileType = null;
    private String mimeHeader = null;
    private String largeMessageUrl = null;
    private boolean base64 = false;
    private boolean usesLarge = false;
    private String attachmentFile = null;
    private boolean largeSingleAttachment = false;
    private boolean compressed = false;
    private boolean externalBase64 = false;
    private String domainSpecificData = null;
    private String contentId = null;
    private long contentLength = 0L;
    private long attachmentLength = 0L;
    private int splitCount = 0;
    private HL7Interaction hl7 = null;

    private String mimeContentDescription = null;
    
    public MultiAttachmentMessageComponentDescriptor(HL7Interaction h, String[] d)
        throws MessageOriginatorException
    {
        init();
        hl7 = h;
        sourceFile = d[MultiAttachmentDescriptorFile.FILE];
        fileType = d[MultiAttachmentDescriptorFile.TYPE];
        base64 = d[MultiAttachmentDescriptorFile.B64].equalsIgnoreCase("y");
        usesLarge = d[MultiAttachmentDescriptorFile.LARGE].equalsIgnoreCase("y");
        largeSingleAttachment = d[MultiAttachmentDescriptorFile.LARGE_SINGLE_ATTACHMENT].equalsIgnoreCase("y");
        compressed = d[MultiAttachmentDescriptorFile.COMPRESSED].equalsIgnoreCase("y");
        externalBase64 = d[MultiAttachmentDescriptorFile.EXTERNALBASE64].equalsIgnoreCase("y");
        domainSpecificData = d[MultiAttachmentDescriptorFile.DOMAIN_SPECIFIC_DATA];
    }

    public HL7Interaction getHL7Interaction() { return hl7; }
    public String getSourceFile() { return sourceFile; }
    public void setSplitCount(int c) 
    {
        if (c > 0) {
            splitCount = c; 
        }
    }
    public int getSplitCount() { return splitCount; }
    
    public void setContentId(String c) { 
        contentId = c; 
        StringBuilder sb = new StringBuilder("\r\n----=_MIME-Boundary\r\n");
        sb.append("Content-Id: <");
        sb.append(contentId);
        sb.append(">\r\nContent-Type: ");
        if (mimeContentDescription == null) {
            sb.append(fileType);
        } else {
            sb.append(mimeContentDescription);
        }
        sb.append("\r\nContent-Transfer-Encoding: ");
        if (base64 || externalBase64) {
            sb.append("base64");
        } else {
            sb.append("8bit");
        }
        sb.append("\r\n\r\n");
        mimeHeader = sb.toString();
    }
    public String getContentId() { return contentId; }

    public long getAttachmentSize() { return attachmentLength; }
    
    public long getContentLenght() {
        if (usesLarge) return 0L;
        return contentLength + (long)mimeHeader.length(); 
    }
    
    public String getType() { return fileType; }
    public boolean isBase64() { return base64; }
    public boolean usesLarge() { return usesLarge; }
    public boolean isLargeSingleAttachment() { return largeSingleAttachment; }
    public boolean isCompressed() { return compressed; }
    public boolean isExternalBase64() { return externalBase64; }
    public String getDomainSpecificData() { return domainSpecificData; }
    
    public String getLargeMessageUrl() { return largeMessageUrl; }
    
    private void init()
        throws MessageOriginatorException
    {
        if (sourceDir == null) {
            sourceDir = System.getProperty(MULTISOURCE);
            workDir = System.getProperty(MULTIWORK);
            dataDir = System.getProperty(MULTIDATA);
            File f = new File(sourceDir);
            if (!(f.exists() && f.isDirectory())) {
                throw new MessageOriginatorException("Cannot find multipart message source directory " + sourceDir);
            } 
            f = new File(workDir);
            if (!(f.exists() && f.isDirectory())) {
                throw new MessageOriginatorException("Cannot find multipart message working directory " + sourceDir);
            } 
            String b = System.getProperty(B64FILEBUFFER);
            try {
                if (b == null) {
                    base64buffer = DEFAULTB64BUFFER;
                } else {
                    base64buffer = Integer.parseInt(b);
                }
            }
            catch (Exception e) {
                base64buffer = DEFAULTB64BUFFER;
            }
            mimeContentDescription = System.getProperty(LIEABOUTMIMECONTENTDESCRIPTION);
        }
    }
    
    public void preprocess(String target)
        throws MessageOriginatorException
    {
        if (usesLarge) {
            try {
                File f = new File(dataDir, sourceFile);
                attachmentLength = f.length();
                FileInputStream fr = new FileInputStream(f);
                LargeMessagePosterSingleton lmps = LargeMessagePosterSingleton.getInstance();
                largeMessageUrl = lmps.postIntermediate(fr, target, this);
            }
            catch (Exception e) {
                throw new MessageOriginatorException("Failed to post attachment: " + e.getMessage());
            }
        } else {
            if (base64) {
                doBase64();
                return;
            }         
            File f = new File(dataDir, sourceFile);
            if (!f.exists()) {
                throw new MessageOriginatorException("File not found: " + sourceFile);
            }
            contentLength = f.length();
        }
    }   
   
    private void doBase64()
        throws MessageOriginatorException
    {
        File src = new File(dataDir, sourceFile);
        File out = new File(workDir, sourceFile);
        byte buf[] = new byte[base64buffer];
        FileInputStream fIn = null;
        BufferedWriter bw = null;
        
        try {
            fIn = new FileInputStream(src);
            bw = new BufferedWriter(new FileWriter(out));
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Exception preparing file " + sourceFile + " for base64 encoding: " + e.getMessage());
        }
        // TODO: Open the source file, and read it up to "base64buffer" bytes at a time,
        // encoding as we go. Write encoded out to the same file name in the work directory.
        // Once everything is done, find the size of the base 64 output file and save it.
        // If anything goes wrong, catch the exception and re-throw as a MessageOriginatorException
        int r = 0;
        Base64 base64 = new Base64();
        byte b64[] = null;
        try {
            while ((r = fIn.read(buf, 0, base64buffer)) != -1) {
                if (r < base64buffer) {
                    byte bshort[] = new byte[r];
                    for (int i = 0; i < r; i++) {
                        bshort[i] = buf[i];
                    }
                    b64 = base64.encode(bshort);
                } else {
                    b64 = base64.encode(buf);
                }
                writeBase64(bw, b64);                
                Arrays.fill(buf, (byte)0);
            }
        }
        catch (Exception e) {
            try {
                fIn.close();
                bw.close();
            } catch (Exception e1) {}
            throw new MessageOriginatorException("Exception converting file " + sourceFile + " to base64 encoding: " + e.getMessage());
        }
        try {
           fIn.close();
           bw.flush();
           bw.close();
        } catch (Exception e1) {}
        out = new File(workDir, sourceFile);
        contentLength = out.length();
    }
    
    private void writeBase64(BufferedWriter f, byte[] b64) 
        throws Exception
    {
        int j = 0;
        for (int i = 0; i < b64.length; i++) {
            if ((i != 0) && ((i % CHUNKSIZE) == 0)) {
                f.write("\r\n");
            }
            f.write((int)b64[i]);
        }
        f.write("\r\n");
        f.flush();
    }
    
    public void write(FileWriter fw)
        throws MessageOriginatorException
    {
        if (usesLarge)
            return;
        
        // Read the file associated with this part of the message, into the given FileWriter
        File fIn = null;
        if (base64) {
            fIn = new File(workDir, sourceFile);
        } else {
            fIn = new File(dataDir, sourceFile);
        }
        char buf[] = new char[DEFAULTB64BUFFER];
        try {
            fw.write(mimeHeader);
            FileReader fr = new FileReader(fIn);
            int i = 0;
            while ((i = fr.read(buf)) != -1) {
                fw.write(buf, 0, i);
            }
            fr.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to add" + sourceFile + " to multi-attachment message: " + e.getMessage());
        }
    }
}
