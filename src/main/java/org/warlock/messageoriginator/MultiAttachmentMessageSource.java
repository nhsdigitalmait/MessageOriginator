/*
 * MultiAttachmentMessageSource.java
 *
 * Created on 19 November 2007, 10:33
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator;
import org.warlock.messageoriginator.hl7.HL7Interaction;
import java.io.File;
import java.util.ArrayList;
/**
 *
 * @author DAMU2
 */
public class MultiAttachmentMessageSource 
    extends MessageSource
{
    private static final int DEFAULTBURSTSIZE = 5;
    
    private ArrayList<HL7Interaction> interactions = null;
    
    /** Creates a new instance of MultiAttachmentMessageSource */
    public MultiAttachmentMessageSource(String dirPath) 
        throws MessageOriginatorException
    {
        super(dirPath);
        assemble();
    }

    public MultiAttachmentMessageSource(String[] fileSet) 
        throws MessageOriginatorException
    {
        super(fileSet);
        assemble();
    }
    
    public MultiAttachmentMessageSource(String[] fileSet, String dirPath) 
        throws MessageOriginatorException
    {
        super(fileSet, dirPath);
        assemble();
    }
    
    public HL7Interaction[] getInteractions() { 
     
        HL7Interaction[] hl7 = null;
        int limit = 0;

        if (burstSize != 0) {
            if (doneTo == files.size()) {
                return null;
            }
            hl7 = new HL7Interaction[((files.size() - doneTo) >= burstSize) ? burstSize : (files.size() - doneTo)];
            limit = burstSize;
        } else {
            hl7 = new HL7Interaction[files.size()];
            limit = files.size();
        }
        for (int i = 0; i < limit; i++) {
            hl7[i] = interactions.get(doneTo);
            doneTo++;
            if (doneTo == interactions.size()) {
                break;
            }
        }
        return hl7;        
    }
    
    private void assemble()
        throws MessageOriginatorException
    {
        // This makes the HL7interaction instances, reads the descriptors and makes 
        // MultiAttachmentMessageComponentDescriptor instances for them, then adds the 
        // attachment descriptors to the HL7interaction instances.
        // At this point we already know about the "files" ArrayList in the superclass,
        // so we can use this to determine how big the HL7Interaction array needs to be.
        
        interactions = new ArrayList<HL7Interaction>();
        File f = null;
        MultiAttachmentDescriptorFile m = null;
        int i = 0;
        for (String filename : files) {
            f = new File(path, filename);
            m = new MultiAttachmentDescriptorFile(f);
            m.load();
            HL7Interaction h = new HL7Interaction(m.getHL7());
            // Need to do this here because LM needs things like the ASIDs, it is OK
            // to do so because when the MessageOriginator calls load() again, the
            // HL7Interaction instance will know that it has already loaded OK, and
            // will just return.
            h.load();
            interactions.add(h);
            ArrayList<String[]> att = m.getAttachments();
            MultiAttachmentMessageComponentDescriptor ad[] = new MultiAttachmentMessageComponentDescriptor[att.size()];
            int j = 0;
            for (String[] a : m.getAttachments()) {
                MultiAttachmentMessageComponentDescriptor d 
                    = new MultiAttachmentMessageComponentDescriptor(h, a);
                d.preprocess(m.getTarget());
                ad[j] = d;
                ++j;
            }
            h.setAttachments(ad);
            ++i;
        }
    }
}
