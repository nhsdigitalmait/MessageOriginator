/*
 * Transmission.java
 *
 * Created on 15 March 2006, 23:10
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.transmission;
import org.warlock.messageoriginator.MessageOriginatorException;
import org.safehaus.uuid.UUIDGenerator;
import org.safehaus.uuid.UUID;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.SimpleTimeZone;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.net.MalformedURLException;

/**
 *
 * @author murff
 */
public abstract class Transmission {
    
    protected Wrapper wrapper = null;
    protected WrapperHelper whelp = null;
    protected String soapHeader = null;
    private URL toAddress = null;
    private String messageId = null;
    
    private static SimpleDateFormat ISO8601date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    
    /** Creates a new instance of Transmission */
    protected Transmission(Wrapper w) {
        wrapper = w;
        messageId = getUUID().toUpperCase();
        whelp = WrapperHelper.getInstance();
    }
    abstract void buildSOAPHeader() throws MessageOriginatorException;
    abstract void getPreHL7HTTPStreamContent(StringBuilder s, long contentLength)
        throws MessageOriginatorException;
    abstract String getPostHL7HTTPStreamContent();
    public abstract String getSaveFile();
    abstract void resolveHeaderData()
        throws MessageOriginatorException;
    
    abstract String getAction();
    abstract void buildComplete(int l) throws MessageOriginatorException;
    
    abstract String makeBody(String hl7);
    abstract String makeHeader(int contentLength) throws MessageOriginatorException;
    
    public abstract String getToEndPoint();
    
    protected void substitute(StringBuilder sb, String tag, String content)
        throws MessageOriginatorException
    {
        int tagStart = sb.indexOf(tag);
        if (tagStart == -1) 
            throw new MessageOriginatorException("Invalid sustitution tag: " + tag + " - not found");
        int tagEnd = tagStart + tag.length();
        sb.replace(tagStart, tagEnd, content);
    }
    
    String getMessageId() { return messageId; }
    
    protected String getUUID() {
        UUID u = UUIDGenerator.getInstance().generateTimeBasedUUID();
        return u.toString();
    }
    
    protected String getISO8601TimeStamp() {
//        TimeZone tz = TimeZone.getTimeZone("GMT");
        SimpleTimeZone tz = new SimpleTimeZone(0, "GMT");
//        boolean b = tz.inDaylightTime(new Date());
        Calendar c = Calendar.getInstance(tz);
        Date d = c.getTime();
        return ISO8601date.format(d);
    }
    
    private synchronized void setURL()
        throws MessageOriginatorException
    {
        try {
            toAddress = new URL(getToEndPoint());
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Wrapper endpoint URL: " + getToEndPoint() + " is invalid");
        }
    }
    
    protected String getPath() 
        throws MessageOriginatorException
    {
        if (toAddress == null)
            setURL();
        return toAddress.getPath();
    }
    
    protected String getHost() 
        throws MessageOriginatorException
    {
        if (toAddress == null)
            setURL();
        return toAddress.getHost();
     }
}
