/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.lm;
import org.warlock.messageoriginator.MessageOriginatorException;
import java.io.InputStream;
/**
 *
 * @author damu2
 */
public interface LMEncryptor {

    public void setTarget(String s) throws MessageOriginatorException;
    public InputStream encrypt(InputStream r) throws MessageOriginatorException;
    public void releaseAttachmentResources();
}
