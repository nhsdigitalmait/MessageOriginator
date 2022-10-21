/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.lm;
import org.warlock.messageoriginator.MessageOriginatorException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.crypto.Cipher;
// import javax.crypto.CipherInputStream;
/**
 *
 * @author damu2
 */
public class CertificateFileRSAStreamEncryptor 
    implements LMEncryptor
{
    private static final String ENCRYPTORFILEPROPERTY = "messageoriginator.encryptor.certdir";
    private static final String ENCRYPTORTEMPDIRPROPERTY = "messageoriginator.encryptor.temporarydirectory";
    private static final String ENCRYPTORREADBLOCKSIZEPROPERTY = "messageoriginator.readblocksize";
    private static final int BUFFER = 1024;
    private static final int DEFAULTRSAENCRYPTIONREADBLOCKSIZE = 116;
    
    private Cipher rsaCipher = null;
    private String target = null; 
    private Certificate encryptorCert = null;
    private String tempdir = null;
    private String tempfile = null;
    private int readBlockSize = 0;
    
    // TODO: Add signing... probably not to this, but somewhere sensible...
    
    public CertificateFileRSAStreamEncryptor() 
            throws MessageOriginatorException
    {
        if ((tempdir = System.getProperty(ENCRYPTORTEMPDIRPROPERTY)) == null) {
            throw new MessageOriginatorException("No temporary directory set: " + ENCRYPTORTEMPDIRPROPERTY);
        }
        File f = new File(tempdir);
        if ((!f.exists()) || (!f.isDirectory()) || (!f.canWrite()) || (!f.canExecute())) {
            throw new MessageOriginatorException("Temporary directory " + tempdir + " does not exist or is inaccessible");
        }
        if (System.getProperty(ENCRYPTORREADBLOCKSIZEPROPERTY) == null) {
            System.err.println("No " + ENCRYPTORREADBLOCKSIZEPROPERTY + " set, using default " + DEFAULTRSAENCRYPTIONREADBLOCKSIZE);
            readBlockSize = DEFAULTRSAENCRYPTIONREADBLOCKSIZE;
        } else {
            try {
                readBlockSize = Integer.parseInt(System.getProperty(ENCRYPTORREADBLOCKSIZEPROPERTY));
            }
            catch (Exception e) {
                throw new MessageOriginatorException("Exception reading " + ENCRYPTORREADBLOCKSIZEPROPERTY + " : " + e.getMessage());
            }
        }
    }
    
    public void releaseAttachmentResources()
    {
        if (tempfile != null) {
            try {
                File f = new File(tempfile);
                if (!f.delete()) {
                    System.err.println("Attempt to delete " + tempfile + " reports failure but no exception");
                }
            }
            catch(Exception e) {
                System.err.println("Exception " + e.getMessage() + " trying to remove temporary encryptor file " + tempfile);
            }
        }
    }
    
    public InputStream encrypt(InputStream r) 
            throws MessageOriginatorException
    {
        try {
            // Wailing and gnashing of teeth. The CipherInputStream doesn't seem
            // to do what it is advertised to do (at least not with the rsaCipher),
            // a call to read() returns -1 which isn't what we want.
            // return new CipherInputStream(r, rsaCipher);
            //
            FileInputStream f = null;
            StringBuilder sb = new StringBuilder(tempdir);
            sb.append("/");
            sb.append(Long.toHexString(System.nanoTime()));
            sb.append(sb.hashCode());
            sb.append(".tmp");
            tempfile = sb.toString();
            FileOutputStream out = new FileOutputStream(tempfile);
            //
            // TODO: Finish this. Read from the input, encrypt in blocks, and write to
            // "out". Once that is done, close everything then make a FileInputStream
            // reading the temporary file, and return that.
            //
            byte ptext[] = new byte[readBlockSize];
            byte ctext[] = null;
            int rcount = 0;
            java.util.Arrays.fill(ptext, (byte)0);
            while ((rcount = r.read(ptext, 0, readBlockSize)) != -1) {
                if (rcount != 0) {
                    ctext = rsaCipher.doFinal(ptext, 0, rcount);
                    java.util.Arrays.fill(ptext, (byte)0);
                    out.write(ctext);
                }
            }
            out.close();
            r.close();
            f = new FileInputStream(tempfile);
            return f;
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to create CipherInputStream for posting: " + e.getMessage() + " : " + e.getCause().getMessage());
        }
    }
    
    public void setTarget(String t)
            throws MessageOriginatorException
    {
        if (target != null)
            return;
        if ((t == null) || (t.trim().length() == 0)) {
            throw new MessageOriginatorException("Invalid null or empty target");
        }
        if (encryptorCert != null) {
            throw new MessageOriginatorException("Attempt to re-set target " + t + " for already initialised encryptor");
        }
        target = t.trim();
        init();
    }
    
    private void init()
            throws MessageOriginatorException
    {
        String encryptorFile = System.getProperty(ENCRYPTORFILEPROPERTY);
        if (encryptorFile == null)
            throw new MessageOriginatorException("Encryptor certificate directory not defined: set " + ENCRYPTORFILEPROPERTY);
        
        StringBuilder sb = new StringBuilder(encryptorFile);
        sb.append("/");
        sb.append(target);
        encryptorFile = sb.toString();
        File f = new File(encryptorFile);
        if (!f.exists() || !f.isFile() || !f.canRead()) {
            throw new MessageOriginatorException("Requested encryptor certificate file " + encryptorFile + " inaccessible");
        }
        
        try {
            CertificateFactory c = CertificateFactory.getInstance("X509");
            FileInputStream fis = new FileInputStream(f);
            BufferedInputStream bis = new BufferedInputStream(fis);
            while(bis.available() > 0) {
                encryptorCert = c.generateCertificate(bis);
            }
            bis.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Error reading encryptor certificate file " + encryptorFile + ": " + e.getMessage());
        }
        try {
            // java.security.Provider p[] = java.security.Security.getProviders();
            rsaCipher = Cipher.getInstance("RSA");
            rsaCipher.init(Cipher.ENCRYPT_MODE, encryptorCert);
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to make Cipher from " + encryptorFile + ": " + e.getLocalizedMessage());
        }
    }
}
