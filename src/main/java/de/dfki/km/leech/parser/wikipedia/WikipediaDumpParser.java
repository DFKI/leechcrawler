/*
    Leech - crawling capabilities for Apache Tika
    
    Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Contact us by mail: christian.reuschling@dfki.de
*/

package de.dfki.km.leech.parser.wikipedia;



import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.km.leech.util.MultiValueHashMap;



/**
 * A parser implementation that can deal with mediawiki xml dump files. Downloadable e.g. under:<br>
 * <br>
 * German wikipedia <br>
 * http://dumps.wikimedia.org/dewiki/<br>
 * http://dumps.wikimedia.org/dewiki/latest/dewiki-latest-pages-articles.xml.bz2<br>
 * English wikipedia:<br>
 * http://dumps.wikimedia.org/enwiki/<br>
 * http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class WikipediaDumpParser implements Parser
{

    private static final long serialVersionUID = -7801896202662990477L;



    /**
     * Reads all next character events from an xmlEventReader and concatenate their data into one String
     * 
     * @param xmlEventReader the xmlEventReader to get the events
     * 
     * @return the data of the character events, concatenated into one String
     * 
     * @throws XMLStreamException
     */
    static String readNextCharEventsText(XMLEventReader xmlEventReader) throws XMLStreamException
    {
        StringBuilder strbText = new StringBuilder("");


        while (xmlEventReader.hasNext())
        {
            XMLEvent nextEvent = xmlEventReader.peek();
            if(!nextEvent.isCharacters()) break;

            nextEvent = xmlEventReader.nextEvent();
            strbText.append(nextEvent.asCharacters().getData());
        }


        return strbText.toString();
    }



    public MultiValueHashMap<String, String> getPageTitle2Redirects(InputStream sWikipediaDump) throws FileNotFoundException, XMLStreamException
    {
        // <text xml:space="preserve">#REDIRECT [[Autopoiesis]]</text>
        // <text xml:space="preserve">#REDIRECT:[[Hans Leo Haßler]]</text>
        // <text xml:space="preserve">#redirect [[Weißer Hai]]</text>
        // #weiterleitung
        // <page>
        // <title>Autopoiesis</title>


        MultiValueHashMap<String, String> hsPageTitle2Redirects = new MultiValueHashMap<String, String>();
        HashSet<String> hsRedirectPageTitles = new HashSet<String>();

        String strCurrentTitle = "";
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();

        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(sWikipediaDump, "Utf-8");
        int iTitlesRead = 0;
        while (xmlEventReader.hasNext())
        {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();


            if(!xmlEvent.isStartElement()) continue;
            // wenn wir einen Title haben, dann merken wir uns den, falls wir ihn brauchen
            if(xmlEvent.asStartElement().getName().getLocalPart().equals("title"))
            {
                strCurrentTitle = readNextCharEventsText(xmlEventReader);

                iTitlesRead++;
                if(iTitlesRead % 10000 == 0) System.out.println("read doc #" + iTitlesRead);

                continue;
            }

            if(!xmlEvent.asStartElement().getName().getLocalPart().equals("text")) continue;

            // jetzt haben wir ein text-tag. Wir schauen, ob jetzt ein redirect kommt
            // entweder kommt ein charEvent oder ein EndEvent. Leere Texte gibts wohl auch
            XMLEvent nextEvent = xmlEventReader.peek();

            if(!nextEvent.isCharacters()) continue;

            String strCharEventData = readNextCharEventsText(xmlEventReader);
            if(strCharEventData == null
                    || ((strCharEventData.trim().length() < 10 || !strCharEventData.trim().substring(0, 9).toLowerCase().startsWith("#redirect")) && (strCharEventData
                            .trim().length() < 15 || !strCharEventData.trim().substring(0, 14).toLowerCase().startsWith("#weiterleitung"))))
                continue;

            // wir haben einen redirect - der wird in unsere Datenstruktur eingetragen
            int iStart = strCharEventData.indexOf("[[");
            int iEnd = strCharEventData.indexOf("]]");
            if(iStart < 0 || iEnd < 0) continue;
            if(iEnd <= iStart) continue;
            if((iStart + 2) > strCharEventData.length() || iEnd > strCharEventData.length()) continue;

            String strRedirectTarget = strCharEventData.substring(iStart + 2, iEnd).trim();
            hsPageTitle2Redirects.add(strRedirectTarget, strCurrentTitle);
            hsRedirectPageTitles.add(strCurrentTitle);

            System.out.println("redirect found: (" + hsRedirectPageTitles.size() + ") " + strCurrentTitle + " => '" + strRedirectTarget + "'");
        }



        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.println("Redirects found: " + hsPageTitle2Redirects.valueSize());



        return hsPageTitle2Redirects;

    }



    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context)
    {
        return Collections.singleton(MediaType.application("wikipedia+xml"));
    }



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException,
            TikaException
    {

        try
        {

            // wir iterieren schön über die page-Einträge. Darin gibt es dann title, timestamp, <contributor> => <username> und text. den text müssen
            // wir noch bereinigen. dazu nehmen wir eine Vorverarbeitung mit bliki - dazu müssen wir aber selbst nochmal den String vorbereiten und
            // nachbereinigen. Leider.



            TikaInputStream tikaStream = TikaInputStream.get(stream);
            File fWikipediaDumpFile4Stream = tikaStream.getFile();
            MultiValueHashMap<String, String> hsPageTitle2Redirects = getPageTitle2Redirects(new FileInputStream(fWikipediaDumpFile4Stream));
            HashSet<String> hsRedirectPageTitles = new HashSet<String>(hsPageTitle2Redirects.values());

            String strCleanedText = "";
            String strBaseURL = null;

            int iTitlesRead = 0;


            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(fWikipediaDumpFile4Stream), "Utf-8");
            while (xmlEventReader.hasNext())
            {


                XMLEvent xmlEvent = xmlEventReader.nextEvent();


                if(xmlEvent.isEndElement() && xmlEvent.asEndElement().getName().getLocalPart().equals("page"))
                {
                    if(metadata.size() == 0) continue;

                    // den mimetype wollen wir auch noch in den Metadaten haben
                    metadata.add(Metadata.CONTENT_TYPE, "application/wikipedia+xml");

                    XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
                    xhtml.startDocument();

                    xhtml.startElement("p");
                    xhtml.characters(strCleanedText.toCharArray(), 0, strCleanedText.length());
                    xhtml.endElement("p");

                    xhtml.endDocument();


                }




                if(!xmlEvent.isStartElement()) continue;



                // ##### die siteinfo

                if(strBaseURL == null && xmlEvent.asStartElement().getName().getLocalPart().equals("base"))
                {
                    // http://de.wikipedia.org/wiki/Wikipedia:Hauptseite =>http://de.wikipedia.org/wiki/
                    strBaseURL = readNextCharEventsText(xmlEventReader);
                    strBaseURL = strBaseURL.substring(0, strBaseURL.lastIndexOf("/") + 1);
                }





                // ##### die page

                if(xmlEvent.asStartElement().getName().getLocalPart().equals("page"))
                {
                    for (String strKey : metadata.names())
                        metadata.remove(strKey);
                }



                // ##### der Title

                if(xmlEvent.asStartElement().getName().getLocalPart().equals("title"))
                {
                    // wir merken uns immer den aktuellen Titel
                    String strCurrentTitle = readNextCharEventsText(xmlEventReader);

                    // wenn der Titel eine redirect-Page ist, dann tragen wir die ganze Page aus der EventQueue aus, springen an das endPage, und
                    // haben somit diese Seite ignoriert. Ferner ignorieren wir auch spezielle wikipedia-Seiten
                    String strSmallTitle = strCurrentTitle.trim().toLowerCase();
                    if(hsRedirectPageTitles.contains(strCurrentTitle) || strSmallTitle.startsWith("category:")
                            || strSmallTitle.startsWith("kategorie:") || strSmallTitle.startsWith("vorlage:")
                            || strSmallTitle.startsWith("template:") || strSmallTitle.startsWith("hilfe:") || strSmallTitle.startsWith("help:")
                            || strSmallTitle.startsWith("wikipedia:") || strSmallTitle.startsWith("portal:")
                            || strSmallTitle.startsWith("mediawiki:"))
                    {

                        while (true)
                        {
                            XMLEvent nextXmlEvent = xmlEventReader.nextEvent();
                            if(nextXmlEvent.isEndElement() && nextXmlEvent.asEndElement().getName().getLocalPart().equals("page")) break;
                        }
                    }

                    metadata.add(DublinCore.TITLE, strCurrentTitle);
                    metadata.add(DublinCore.SOURCE, strBaseURL + strCurrentTitle);

                    for (String strRedirect : hsPageTitle2Redirects.get(strCurrentTitle))
                        metadata.add(DublinCore.TITLE, strRedirect);


                    iTitlesRead++;
                    if(iTitlesRead % 10000 == 0) System.out.println("read doc #" + iTitlesRead);

                    continue;
                }




                // ##### der text
                if(xmlEvent.asStartElement().getName().getLocalPart().equals("text"))
                {
                    String strText = readNextCharEventsText(xmlEventReader);

                    // aufgrund einiger Defizite in dem verwendeten cleaner müssen wir hier leider noch zu-und nacharbeiten
                    strText = strText.replaceAll("==\n", "==\n\n");
                    strText = strText.replaceAll("\n==", "\n\n==");

                    WikiModel wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");
                    strCleanedText = wikiModel.render(new PlainTextConverter(), strText);

                    strCleanedText = strCleanedText.replaceAll("\\{\\{", " ");
                    strCleanedText = strCleanedText.replaceAll("\\}\\}", " ");

                    strCleanedText = StringEscapeUtils.unescapeHtml(strCleanedText);

                    continue;
                }



                // ##### der timestamp
                if(xmlEvent.asStartElement().getName().getLocalPart().equals("timestamp"))
                {
                    String strTimestamp = readNextCharEventsText(xmlEventReader);

                    metadata.add(DublinCore.MODIFIED, strTimestamp);

                    continue;
                }



                // ##### der username
                if(xmlEvent.asStartElement().getName().getLocalPart().equals("username"))
                {
                    String strUsername = readNextCharEventsText(xmlEventReader);

                    metadata.add(DublinCore.CREATOR, strUsername);

                    continue;
                }







            }

        }
        catch (Exception e)
        {
            Logger.getLogger(WikipediaDumpParser.class.getName()).log(Level.SEVERE, "Error", e);
        }





    }

}
