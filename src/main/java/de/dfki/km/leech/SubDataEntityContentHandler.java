package de.dfki.km.leech;



import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;



public class SubDataEntityContentHandler extends XHTMLContentHandler
{

    protected String m_strBodyText;



    public SubDataEntityContentHandler(ContentHandler handler, Metadata metadata, String strBodyText)
    {
        super(handler, metadata);
        m_strBodyText = strBodyText;

    }



    public void triggerSubDataEntityHandling() throws SAXException
    {

        startDocument();

        startElement("p");
        characters(m_strBodyText.toCharArray(), 0, m_strBodyText.length());
        endElement("p");

        endDocument();

    }



}
