/*
 * MessageSource.java
 *
 * Created on 21 March 2006, 12:31
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator;
import org.warlock.messageoriginator.hl7.HL7Interaction;
import java.util.ArrayList;
import java.io.File;
/**
 *
 * @author damu2
 */
public class MessageSource {
    
    protected static final int DEFAULTBURSTSIZE = 12;
    
    protected String path = null;
    protected ArrayList<String> files = null;
    protected int burstSize = 0;
    protected int doneTo = 0;
    /** 
     * Creates a new instance of MessageSource from a directory. We assume that this directory
     * contains all the files to be sent.
     */
    public MessageSource(String dirPath) 
        throws MessageOriginatorException
    {
        try {
            burstSize = Integer.parseInt(System.getProperty("messageoriginator.burstsize"));
        }
        catch (Exception e) {
            burstSize = DEFAULTBURSTSIZE;
        }
        setPath(dirPath);
        File f = new File(dirPath);
        setFiles(f.list());

    }
    
    /**
     * New messagesource from a set of files where the path is specified for each.
     */
    public MessageSource(String[] fileSet) 
        throws MessageOriginatorException
    {
        setFiles(fileSet);
    }
    
    /**
     * New messagesource from a set of files all in the given path.
     */
    public MessageSource(String[] fileSet, String dirPath) 
        throws MessageOriginatorException
    {
        setPath(dirPath);
        setFiles(fileSet);
    }
    
    private void setFiles(String[] fileSet) {
        files = new ArrayList<String>(fileSet.length);
        for (int i = 0; i < fileSet.length; i++)
            files.add(fileSet[i]);
    }
    
    private void setPath(String dirPath)
        throws MessageOriginatorException
    {
        if (dirPath == null) 
            throw new MessageOriginatorException("Directory message source may not be null");
        if (dirPath.length() == 0)
            throw new MessageOriginatorException("Directory message source may not be empty");
        File f = new File(dirPath);
        if (!f.exists()) 
            throw new MessageOriginatorException("Directory message source not found: " + dirPath);
        if (!f.isDirectory())
            throw new MessageOriginatorException("Directory message source not a directory: " + dirPath);
        f = null;
        path = dirPath;
    }
    
    public HL7Interaction[] getInteractions()
    {
        
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
            if (path == null) {
               hl7[i] = new HL7Interaction(files.get(doneTo));
            } else {
               hl7[i] = new HL7Interaction(files.get(doneTo), path); 
            }
            doneTo++;
            if (doneTo == files.size()) {
                break;
            }
        }
        return hl7;
    }
}
