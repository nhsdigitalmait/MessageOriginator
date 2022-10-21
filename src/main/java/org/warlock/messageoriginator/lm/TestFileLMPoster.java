/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.lm;
import org.warlock.messageoriginator.MultiAttachmentMessageComponentDescriptor;
import org.warlock.messageoriginator.MessageOriginatorException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.text.SimpleDateFormat;
/**
 *
 * @author DAMU2
 */
public class TestFileLMPoster 
    implements LMIntermediatePoster
{
    private String fileRoot = null;
    private String hrefRoot = null;
    private int sequence = 0;
    private MessageOriginatorException bootException = null;
    
    private static final int BUFFER = 1024;
    private static final SimpleDateFormat ROOTFORMAT = new SimpleDateFormat("yyyyMMddHHmmssSSS");
    private static final String TESTFILELMPOSTERDIRECTORYPROPERTY = "messageoriginator.lm.testposterdir";
    
    public TestFileLMPoster()
            throws MessageOriginatorException
    {
        String directory = System.getProperty(TESTFILELMPOSTERDIRECTORYPROPERTY);
        if (directory == null) {
            MessageOriginatorException e = new MessageOriginatorException("Test poster directory not set - add " + TESTFILELMPOSTERDIRECTORYPROPERTY + " to the properties file");
            bootException  = e;
            throw e;
        }        
        File f = new File(directory);
        if (!f.exists() || !f.isDirectory() || !f.canWrite()) {
            MessageOriginatorException e = new MessageOriginatorException("Test poster directory does not exist or is not accessible: " + TESTFILELMPOSTERDIRECTORYPROPERTY);
            bootException  = e;
            throw e;            
        }
        StringBuilder sbf = new StringBuilder(directory);
        sbf.append("/");
        String d = ROOTFORMAT.format(new Date()); 
        sbf.append(d);
        sbf.append("_");
        fileRoot = sbf.toString();
        hrefRoot = "file:///" + sbf.toString();
    }
    
    public String post(InputStream r, MultiAttachmentMessageComponentDescriptor d)
            throws MessageOriginatorException  
    {
        if (bootException != null) {
            throw bootException;
        }
        StringBuilder sbf = new StringBuilder(fileRoot);
        StringBuilder sbh = new StringBuilder(hrefRoot);
        sbf.append(sequence);
        sbh.append(sequence);
        sequence++;
        sbf.append(".messageattachment");
        sbh.append(".messageattachment");
        String filename = sbf.toString();
        String href = sbh.toString();
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
        return href;
    }
}
