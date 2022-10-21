/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.lm;
import org.warlock.messageoriginator.MessageOriginatorException;
import org.warlock.messageoriginator.MultiAttachmentMessageComponentDescriptor;
import java.io.InputStream;
/**
 *
 * @author damu2
 */
public class LargeMessagePosterSingleton {

    private static LargeMessagePosterSingleton me = new LargeMessagePosterSingleton();
    private static Exception bootTimeException = null;
    
    private static final String POSTERCLASSPROPERTY = "messageoriginator.lm.posterclass";
    private static final String CRYPTOCLASSPROPERTY = "messageoriginator.lm.cryptoclass";
    public static final String POSTERCLASSINTERACTIONAWAREPROPERTY = "messageoriginator.lm.posterclass.isinteractionaware";
    
    private LMIntermediatePoster poster = null;
    private LMEncryptor encryptor = null;
    
    private LargeMessagePosterSingleton() {
        try {
            if (System.getProperty(POSTERCLASSPROPERTY) == null) {
                throw new Exception(POSTERCLASSPROPERTY + " not set in properties file");
            }
            poster = (LMIntermediatePoster)Class.forName(System.getProperty(POSTERCLASSPROPERTY)).newInstance();
            if (System.getProperty(CRYPTOCLASSPROPERTY) == null) {
                System.err.println("WARNING: No encryptor class set in LM posting interface - check transmitter properties");
            } else {
                encryptor = (LMEncryptor)Class.forName(System.getProperty(CRYPTOCLASSPROPERTY)).newInstance();    
            }
        }
        catch (Exception e) {
            bootTimeException = e;
        }
    }
    
    public static LargeMessagePosterSingleton getInstance()
            throws Exception
    {
        if (bootTimeException != null)
            throw bootTimeException;
        return me;
    }
        
    public String postIntermediate(InputStream fr, String target, MultiAttachmentMessageComponentDescriptor d)
            throws MessageOriginatorException
    {
        InputStream r = fr;
        if (encryptor != null) {
            encryptor.setTarget(target);
            r = encryptor.encrypt(fr);
        }
        String url = poster.post(r, d);
        if (encryptor != null){
            encryptor.releaseAttachmentResources();
        }
        return url;
    }
}
