/*
 * XMLHelperNamespaceContext.java
 *
 * Created on 17 March 2006, 12:57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.warlock.messageoriginator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import javax.xml.XMLConstants;
/**
 *
 * @author murff
 */
public class XMLHelperNamespaceContext 
    implements javax.xml.namespace.NamespaceContext
{
    
    private String defaultUri = null;
    private HashMap<String,ArrayList<String>> uris;
    
    /** Creates a new instance of XMLHelperNamespaceContext */
    public XMLHelperNamespaceContext() {
        uris = new HashMap<String,ArrayList<String>>();
    }
    
    public void declarePrefix(String prefix, String uri) 
        throws IllegalArgumentException
    {
        if ((uri == null) || (prefix == null)) {
            throw new IllegalArgumentException("Namespace prefix and URI may not be null");
        }
        if (prefix.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            defaultUri = uri;
        }
        if (uris.containsKey(uri)) {
            ArrayList<String> a = uris.get(uri);
            a.add(prefix);
        } else {
            ArrayList<String> a = new ArrayList<String>();
            a.add(prefix);
            uris.put(uri, a);
        }
    }
    
    public String getPrefix(String u) 
        throws IllegalArgumentException
    {
        if (u == null)
            throw new IllegalArgumentException();
        if (u.equals(XMLConstants.XML_NS_URI)) {
            return XMLConstants.XML_NS_PREFIX;
        }
        if (u.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return XMLConstants.XMLNS_ATTRIBUTE;
        }
        if (!uris.containsKey(u))
            return null;
        return uris.get(u).get(0);
    }
    
    public Iterator getPrefixes(String u)
        throws IllegalArgumentException
    {
        if (u == null)
            throw new IllegalArgumentException();
        if (u.equals(XMLConstants.XML_NS_URI)) {
            return makeSingleItemIterator(XMLConstants.XML_NS_PREFIX);
        }
        if (u.equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
            return makeSingleItemIterator(XMLConstants.XMLNS_ATTRIBUTE);
        }
        if (!uris.containsKey(u))
            return makeSingleItemIterator(null);
        return uris.get(u).iterator();
    }
    
    private Iterator<String> makeSingleItemIterator(String s)
    {
       ArrayList<String> a = new ArrayList<String>();
       if (s != null)
        a.add(s);
       return a.iterator();       
    }
    
    public String getNamespaceURI(String p)
        throws IllegalArgumentException
    {
        if (p == null)
            throw new IllegalArgumentException();
        if (p.equals(XMLConstants.DEFAULT_NS_PREFIX)) {
            return defaultUri;
        }
        if (p.equals(XMLConstants.XML_NS_PREFIX)) {
            return XMLConstants.XML_NS_URI;
        }
        if (p.equals(XMLConstants.XMLNS_ATTRIBUTE)) {
            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
        }
        Iterator<String> it = uris.keySet().iterator();
        while (it.hasNext()) {
            String u = it.next();
            ArrayList<String> a = uris.get(u);
            if (a.contains(p)) {
                return u;
            }
        }
        return XMLConstants.NULL_NS_URI;
    }
}
