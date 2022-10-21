/*
 * OutputManager.java
 *
 * Created on 22 March 2006, 10:27
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator;
import org.warlock.messageoriginator.transmission.Wrapper;
// import org.warlock.messageoriginator.hl7.HL7Interaction;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.URL;
import java.net.MalformedURLException;
/**
 *
 * @author DAMU2
 */
public class OutputManager {
    
    public static final String OUTPUT_NET_PROXY_URL_PROPERTY = "output.proxy.uri";
    private String proxyName = null;
    private URL proxyUrl = null;
    private String outputDirectory = null;
    private boolean doNet = false;
    private boolean doFile = false;
    
    public OutputManager(String outDir)
        throws MessageOriginatorException
    {
        initFileOutput(outDir);
    }
    
    /** Creates a new instance of OutputManager */
    public OutputManager(String outDir, String proxy) 
        throws MessageOriginatorException
    {
        initFileOutput(outDir);
        initProxy(proxy);
    }
    
    public void setProxy(String proxy)
        throws MessageOriginatorException
    {
        initProxy(proxy);
    }
    
    private void initProxy(String proxy)
        throws MessageOriginatorException
    {
        if (proxy != null) {
            try {
                proxyUrl = new URL(proxy);
            }
            catch (MalformedURLException e) {
                throw new MessageOriginatorException("Proxy URL: " + proxy + " is invalid.");
            }
            proxyName = proxy;
            doNet = true;
        }        
    }
    
    private void initFileOutput(String outDir)
        throws MessageOriginatorException
    {
        if (outDir != null) {
            File f = new File(outDir);
            if (!f.exists())
                throw new MessageOriginatorException("Given output location " + outDir + " does not exist.");
            if (!f.isDirectory())
                throw new MessageOriginatorException("Given output location " + outDir + " is not a directory");
            f = null;
            outputDirectory = outDir;
            doFile = true;
        }        
    }
    
    public String getOutputDirectory() { 
        if (doFile) {
            return outputDirectory; 
        } else {
            return "No file output";
        }
    }
    
    public String getProxyName() {
        if (doNet) {
            return proxyName;
        } else {
            return "Not sending to a proxy";
        }
    }
    
    public void setDoFile(boolean b) { doFile = true; }
    public void setDoNet(boolean b) { doNet = false; }
    public boolean getDoFile() { return doFile; }
    public boolean getDoNet() { return doNet; }
    
    public void output(Wrapper w) 
        throws MessageOriginatorException
    {
        String transmission = w.serialiseTransmission();
        if (transmission == null) {
            // Multi-attachment
            if (doNet) {
                send(w, transmission);
            }
        } else {
            if (doFile) {
                write(w, transmission);
            }
            if (doNet) {
                send(w, transmission);
            }
        }
    }
    
    private String makeFileName(Wrapper w, String direction) {
        File f = new File(w.getHL7().getFileName());
        StringBuilder fName = new StringBuilder(outputDirectory);
        //fName.append(System.getProperty("file.separator"));
        fName.append("/");
        fName.append(f.getName());
        fName.append(direction);
        f = null;
        return fName.toString();
    }
    
    private void write(Wrapper w, String transmission)
        throws MessageOriginatorException
    {
        String fName = makeFileName(w, ".out");
        try {
//            FileWriter fOut = new FileWriter(fName);
//            fOut.write(transmission);
            FileOutputStream fOut = new FileOutputStream(fName);
            fOut.write(transmission.getBytes());
            fOut.flush();
            fOut.close();
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to write to file output " + fName + ": " + e.getMessage());
        }
    }
    
    private void send(Wrapper w, String transmission)
        throws MessageOriginatorException
    {
        String fName = makeFileName(w, ".in");
        try {
            new HttpPoster(proxyUrl, transmission, this, w, fName);
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to send message to HTTP proxy: " + e.getMessage());
        }
    }
    
    synchronized void sendLogger(Exception e) {
        System.err.println("Error sending message to proxy: " + e.getMessage());
    }
    
}
