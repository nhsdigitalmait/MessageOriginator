/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.lm;
import org.warlock.messageoriginator.MultiAttachmentMessageComponentDescriptor;
import org.warlock.messageoriginator.MessageOriginatorException;
import java.io.InputStream;
/**
 *
 * @author damu2
 */
public interface LMIntermediatePoster {

    public String post(InputStream r, MultiAttachmentMessageComponentDescriptor d) throws MessageOriginatorException;            
}
