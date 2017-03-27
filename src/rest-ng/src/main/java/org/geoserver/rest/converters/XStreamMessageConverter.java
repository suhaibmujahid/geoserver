package org.geoserver.rest.converters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;

import org.geoserver.rest.RequestInfo;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Base class for XStream based message converters
 */
public abstract class XStreamMessageConverter extends BaseMessageConverter {

    static final Logger LOGGER = Logging.getLogger(XStreamMessageConverter.class);
    
    public XStreamMessageConverter(ApplicationContext applicationContext) {
        super(applicationContext);
    }


    /**
     * Encode the given link
     * @param link
     * @param writer
     */
    public abstract void encodeLink( String link, HierarchicalStreamWriter writer);
    
    /**
     * Encode the given link
     * @param link
     * @param writer
     */
    public abstract void encodeCollectionLink( String link, HierarchicalStreamWriter writer);


    /**
     * Create the instance of XStream needed to do encoding
     * @return
     */
    protected abstract XStream createXStreamInstance();

    protected void encodeAlternateAtomLink(String link, HierarchicalStreamWriter writer) {
        writer.startNode( "atom:link");
        writer.addAttribute("xmlns:atom", "http://www.w3.org/2005/Atom");
        writer.addAttribute("rel", "alternate");
        writer.addAttribute("href", href(link));
        writer.addAttribute("type", getMediaType());

        writer.endNode();
    }

    protected String href( String link) {

        final RequestInfo pg = RequestInfo.get();
        String ext = getExtension();

        if(ext != null && ext.length() > 0)
            link = link+ "." + ext;

        // encode as relative or absolute depending on the link type
        if ( link.startsWith( "/") ) {
            // absolute, encode from "root"
            return pg.servletURI(link);
        } else {
            //encode as relative
            return pg.pageURI(link);
        }
    }
    
    public String encode(String component) {
        try {
            return URLEncoder.encode(component, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.warning("Unable to URL-encode component: " + component);
            return component;
        }
    }

    /**
     * The extension used for resources of the type being encoded
     * @return
     */
    protected abstract String getExtension();

    /**
     * Get the text representation of the mime type being encoded. Only used in link encoding for
     * xml
     * @return
     */
    protected abstract String getMediaType();
}