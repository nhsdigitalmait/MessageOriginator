/*
 * MultiAttachmentDescriptorFile.java
 *
 * Created on 21 November 2007, 08:29
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.StringTokenizer;
/**
 *
 * @author DAMU2
 */
class MultiAttachmentDescriptorFile {
    
    // TODO: Modify this to carry a "target" specifier so we can identify the 
    // recipient and grab the correct public key for decrpytion.
    
    private static final int ATTACHMENTPARTS = 8;
    static final int FILE = 0;
    static final int TYPE = 1;
    static final int B64 = 2;
    static final int LARGE = 3;
    static final int LARGE_SINGLE_ATTACHMENT = 4;
    static final int COMPRESSED = 5;
    static final int EXTERNALBASE64 = 6;
    static final int DOMAIN_SPECIFIC_DATA = 7;
    private File descriptor = null;
    private String hl7file = null;
    private String target = null;
    private ArrayList<String[]> attachments = null;
    
    /** Creates a new instance of MultiAttachmentDescriptorFile */
    MultiAttachmentDescriptorFile(File f) 
        throws MessageOriginatorException
    {
        descriptor = f;
        if (!(f.exists() && f.isFile() && f.canRead())) {
            throw new MessageOriginatorException("Descriptor file " + f.getName() + " cannot be read");
        }
        attachments = new ArrayList<String[]>();
    }

    String getHL7() { 
        StringBuilder sb = new StringBuilder(System.getProperty("messageoriginator.datadir"));
        //sb.append(System.getProperty("file.separator"));
        sb.append("/");
        sb.append(hl7file);
        return sb.toString(); 
    }

    String getTarget() { return target; }
    
    ArrayList<String[]> getAttachments() { return attachments; }
    
    void load()
        throws MessageOriginatorException
    {
        String line = null;
        boolean gotHl7 = false;
        try {
            BufferedReader b = new BufferedReader(new FileReader(descriptor));
            while ((line = b.readLine()) != null) {
                if (line.trim().length() == 0) {
                    break;
                }
                StringTokenizer st = new StringTokenizer(line, "\t", false);
                if (!gotHl7) {
                    if (!st.nextToken().equalsIgnoreCase("HL7part")) {
                        throw new MessageOriginatorException("Malformed multipart descriptor file (did not get HL7 part): " + descriptor.getName());
                    }
                    hl7file = st.nextToken();
                    gotHl7 = true;
                    // Add optional "target {target}" to the HL7part line
                    if (st.hasMoreTokens()) {
                        if (st.nextToken().equalsIgnoreCase("target")) {
                            if (st.hasMoreTokens()) {
                                target = st.nextToken();
                            } else {
                                throw new MessageOriginatorException("Multipart descriptor file says it has a target but gives no name");
                            }
                        }
                    }
                } else {
                    String parts[] = new String[ATTACHMENTPARTS];                        
                    parts[FILE] = dieIfNull(st.nextToken());
                    parts[TYPE] = dieIfNull(st.nextToken());
                    parts[B64] = dieIfNull(st.nextToken());
                    parts[LARGE] = dieIfNull(st.nextToken());
                    parts[LARGE_SINGLE_ATTACHMENT] = dieIfNull(st.nextToken());
                    parts[COMPRESSED] = dieIfNull(st.nextToken());
                    parts[EXTERNALBASE64] = dieIfNull(st.nextToken());
                    if (st.hasMoreTokens()) {
                        parts[DOMAIN_SPECIFIC_DATA] = st.nextToken();
                    } else {
                        parts[DOMAIN_SPECIFIC_DATA] = null;
                    }
                    attachments.add(parts);
                }
            }
            b.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Error reading descriptor file " + descriptor.getName() + ": " + e.getMessage());
        }
    }

    String dieIfNull(String s) 
        throws MessageOriginatorException
    {
        if ((s== null) || (s.trim().length() == 0)) {
            throw new MessageOriginatorException("Malformed multipart descriptor file (did not get attachment data): " + descriptor.getName());
        }
        return s;
    }
}
