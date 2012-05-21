package de.dfki.km.leech.sax;

import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

public class TestContentHandler extends BodyContentHandler
{
    @Override
    public void endDocument() throws SAXException
    {
        super.endDocument();
        
        System.out.println(this.toString());
    }
}
