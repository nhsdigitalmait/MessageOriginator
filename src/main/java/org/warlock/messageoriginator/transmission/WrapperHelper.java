/*
 * WrapperHelper.java
 *
 * Created on 17 March 2006, 11:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator.transmission;
import org.warlock.messageoriginator.MessageOriginatorException;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.HashMap;

/**
 *
 * @author murff
 */
public class WrapperHelper {
    
    private static WrapperHelper me = new WrapperHelper();
    private static final String SDSREFERENCE = "messageoriginator.sds.reference";
    private static final String INTERACTIONMAP = "messageoriginator.interaction.map";
    private static final String REFSDSENTRYTAGNAME = "entry";   
    private static final String REFASIDNAME = "asid";
    private static final String REFCPAIDNAME = "cpaid";
    private static final String REFINTERACTIONNAME = "interaction";
    private static final String REFSERVICE = "service";
    private static final String SYNCHRESPONSEONLYSERVICE = "SYNCH-RESPONSE-ONLY";
    private static final String MESSAGEHEADERTEMPLATE = "messageoriginator.message.headertemplate";
    private static final String WSHEADERTEMPLATE = "messageoriginator.webservice.headertemplate";
    
    private NodeList nl = null;
    private HashMap<String,String> interactionMap = null;
    
    /** Creates a new instance of WrapperHelper */
    private WrapperHelper() {
        try {
            loadInteractionMap();
            String rfile = System.getProperty(SDSREFERENCE);
            FileReader fr = new FileReader(rfile);
            InputSource is = new InputSource(fr);
            DOMParser d = new DOMParser();
            d.parse(is);
            Document refDoc = d.getDocument();
            nl = refDoc.getElementsByTagName(REFSDSENTRYTAGNAME);
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(e.getStackTrace());
            System.exit(1);
        }
    }
    
    private void loadInteractionMap() 
        throws MessageOriginatorException
    {
        String fName = System.getProperty(INTERACTIONMAP);
        if (fName == null) 
            throw new MessageOriginatorException("System property " + INTERACTIONMAP + " not set");
        try {
            HashMap<String,String> h = new HashMap<String,String>();
            String line = null;
            FileReader fr = new FileReader(fName);
            BufferedReader br = new BufferedReader(fr);
            while ((line = br.readLine()) != null) {
                int delimiter = line.indexOf(':');
                h.put(line.substring(0, delimiter), line.substring(delimiter + 1));
            }
            fr.close();
            interactionMap = h;
        }
        catch (Exception e) {
            throw new MessageOriginatorException("Failed to read interaction map: " + fName + ": " + e.getMessage());
        }
        
    }
    
    String resolveInteractionType(String interaction)
        throws MessageOriginatorException
    {
        if ((interaction == null) || (interaction.length() == 0)) 
            throw new MessageOriginatorException("Invalid interaction: " + interaction);
        if (!interactionMap.containsKey(interaction))
            throw new MessageOriginatorException("Interaction not found or unsupported: " + interaction);
        return interactionMap.get(interaction);
    }
    
    public static synchronized WrapperHelper getInstance() { return me; }

    public HashMap<String,String> getVirtualSDSEntry(String asid, String interactionid, boolean service) {
        Element sdsElement = getVirtualSDSNode(asid, interactionid, service);
        return getSDSEntry(sdsElement);
    }
    
    private HashMap<String,String> getSDSEntry(Element sdsElement) {
        if (sdsElement == null)
            return null;
        HashMap<String,String> ent = new HashMap<String,String>();
        NamedNodeMap nnm = sdsElement.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            Attr a = (Attr)nnm.item(i);
            ent.put(a.getName(),a.getValue());
        }
        return ent;        
    }
    public HashMap<String,String> getSDSInteractionEntry(String asid, String interactionid) {
        Element sdsElement = getSDSNode(asid, interactionid);
        return getSDSEntry(sdsElement);
    }
    
    public StringBuilder getMessageHeaderTemplate()
        throws MessageOriginatorException
    {
        String fname = System.getProperty(MESSAGEHEADERTEMPLATE);
        return new StringBuilder(getTemplate(fname));
    }
    
    public StringBuilder getWebServiceHeaderTemplate()
        throws MessageOriginatorException
    {
        String fname = System.getProperty(WSHEADERTEMPLATE);
        return new StringBuilder(getTemplate(fname));
    }
    
    private String getTemplate(String fileName)
        throws MessageOriginatorException
    {
        try {
            File f = new File(fileName);
            char[] cBuf = new char[(int)f.length()];
            FileReader fr = new FileReader(f);
            fr.read(cBuf);
            fr.close();
            fr = null;
            f = null;
            return new String(cBuf);
        }
        catch (Exception e) {
            throw new MessageOriginatorException(e.getMessage());
        }
    }

    private Element getVirtualSDSNode(String asid, String interactionid, boolean service) {
        /*
         * This operates in two ways: an interaction id will be passed for a "to" asid,
         * and not for the "from". We won't find the interaction id for the from asid
         * anyway, so we just return the first node for that asid that we find because
         * all we actually want is its MHS endpoint.
         */
        Node n;
        for (int i = 0; i < nl.getLength(); i++) {
            n = nl.item(i);
            Attr a = (Attr)(n.getAttributes().getNamedItem(REFASIDNAME));
            Attr c = (Attr)(n.getAttributes().getNamedItem(REFCPAIDNAME));
            Attr s = (Attr)(n.getAttributes().getNamedItem(REFSERVICE));
            if (c.getValue().equals("")) {
                Attr h = (Attr)(n.getAttributes().getNamedItem(REFINTERACTIONNAME));
                if (!service && s.getValue().equals(SYNCHRESPONSEONLYSERVICE)) {
                  //  if (a.getValue().equals(asid) && h.getValue().equals(interactionid)) {
                    if (a.getValue().equals(asid)) {
                        return (Element)n;
                    }
                } else {
                    if (a.getValue().equals(asid) && h.getValue().equals(interactionid) && !s.getValue().equals(SYNCHRESPONSEONLYSERVICE)) {
                        return (Element)n;
                    }                    
                }
            }
        }
        return null;
    }
    
    private Element getSDSNode(String asid, String interactionid) {
        /*
         * This operates in two ways: an interaction id will be passed for a "to" asid,
         * and not for the "from". We won't find the interaction id for the from asid
         * anyway, so we just return the first node for that asid that we find because
         * all we actually want is its MHS endpoint.
         */
        Node n;
        for (int i = 0; i < nl.getLength(); i++) {
            n = nl.item(i);
            Attr a = (Attr)(n.getAttributes().getNamedItem(REFASIDNAME));
            Attr c = (Attr)(n.getAttributes().getNamedItem(REFCPAIDNAME));
            if (a.getValue().equals(asid)) {
                if (interactionid != null) {
                    Attr h = (Attr)(n.getAttributes().getNamedItem(REFINTERACTIONNAME));
                    if (!c.getValue().equals("") && h.getValue().equals(interactionid)) {
                        return (Element)n;
                    }
                } else {
                    if (!c.getValue().equals("") && a.getValue().equals(asid))
                        return (Element)n;
                }
            }
        }
        return null;
    }
}
