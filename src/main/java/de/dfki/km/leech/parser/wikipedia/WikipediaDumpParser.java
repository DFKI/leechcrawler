/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.parser.wikipedia;



import info.bliki.wiki.filter.PlainTextConverter;
import info.bliki.wiki.model.WikiModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.server.UID;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.inquisition.collections.MultiValueBalancedTreeMap;
import de.dfki.inquisition.collections.MultiValueHashMap;
import de.dfki.inquisition.text.StringUtils;
import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.util.TikaUtils;



/**
 * A parser implementation that can deal with mediawiki xml dump files, downloadable e.g. under:<br>
 * <br>
 * German wikipedia <br>
 * http://dumps.wikimedia.org/dewiki/<br>
 * http://dumps.wikimedia.org/dewiki/latest/dewiki-latest-pages-articles.xml.bz2<br>
 * English wikipedia:<br>
 * http://dumps.wikimedia.org/enwiki/<br>
 * http://dumps.wikimedia.org/enwiki/latest/enwiki-latest-pages-articles.xml.bz2 <br>
 * <br>
 * Configure this parser inside the ParseContext: ParseContext.set(WikipediaDumpParserConfig.class, wikipediaDumpParserConfig);
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class WikipediaDumpParser implements Parser
{

    public static class WikipediaDumpParserConfig
    {
        protected boolean determinePageRedirects = true;



        protected boolean parseGeoCoordinates = true;



        protected boolean parseInfoBoxes = false;



        protected boolean parseLinksAndCategories = false;



        public boolean getDeterminePageRedirects()
        {
            return determinePageRedirects;
        }



        public boolean getParseGeoCoordinates()
        {
            return parseGeoCoordinates;
        }



        public boolean getParseInfoBoxes()
        {
            return parseInfoBoxes;
        }



        public boolean getParseLinksAndCategories()
        {
            return parseLinksAndCategories;
        }



        public WikipediaDumpParserConfig setDeterminePageRedirects(boolean determinePageRedirects)
        {
            this.determinePageRedirects = determinePageRedirects;

            return this;
        }



        public WikipediaDumpParserConfig setParseGeoCoordinates(boolean parseGeoCoordinates)
        {
            this.parseGeoCoordinates = parseGeoCoordinates;

            return this;
        }



        public WikipediaDumpParserConfig setParseInfoBoxes(boolean parseInfoBoxes)
        {
            this.parseInfoBoxes = parseInfoBoxes;

            return this;
        }



        public WikipediaDumpParserConfig setParseLinksAndCategories(boolean parseLinksAndCategories)
        {
            this.parseLinksAndCategories = parseLinksAndCategories;

            return this;
        }
    }


    public static final String externalLink = "externalLink";

    public static final String infobox = "infobox";


    public static final String internalLink = "internalLink";



    static protected final WikiModel m_wikiModel = new WikiModel("http://www.mywiki.com/wiki/${image}", "http://www.mywiki.com/wiki/${title}");



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
    static protected String readNextCharEventsText(XMLEventReader xmlEventReader) throws XMLStreamException
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



    protected Pattern dmsCoordinatePattern = Pattern.compile("(\\d+\\.?\\d*)/(\\d*+\\.?\\d*)/(\\d*\\.?\\d*)/([NESW])");



    protected String cleanAttValue(String strAttName, String strAttValue)
    {
        if(strAttValue == null) strAttValue = "";

        // Angaben in Klammern kommen weg
        strAttValue = strAttValue.replaceAll("\\(.*?\\)", "");
        // Angaben in geschweiften Klammern kommen weg
        strAttValue = strAttValue.replaceAll("\\{\\{.*?\\}\\}", "").trim();

        if("longitude".equals(strAttName) || "latitude".equals(strAttName))
        {
            Matcher degreeMatcher = dmsCoordinatePattern.matcher(strAttValue);

            if(degreeMatcher.find())
            {
                double dInDezimal = dmsToDecCoordinate(degreeMatcher.group(1), degreeMatcher.group(2), degreeMatcher.group(3));

                if("E".equals(degreeMatcher.group(4))) strAttValue = String.valueOf(dInDezimal);
                if("W".equals(degreeMatcher.group(4))) strAttValue = String.valueOf(dInDezimal * -1);
                if("N".equals(degreeMatcher.group(4))) strAttValue = String.valueOf(dInDezimal);
                if("S".equals(degreeMatcher.group(4))) strAttValue = String.valueOf(dInDezimal * -1);
            }
            else
            {

                // wenn es keine Gradzahl ist, müssen wir schauen, ob es auch eine Zahl ist - es gibt auch fehlerhafte Einträge in der Wikipedia...
                try
                {
                    Double.valueOf(strAttValue);
                }
                catch (NumberFormatException e)
                {
                    return null;
                }
            }
        }

        return strAttValue;
    }



    /**
     * Converts DMS ( Degrees / minutes / seconds ) to decimal format longitude / latitude
     * 
     * @param strDegree
     * @param strMinutes
     * @param strSeconds
     * 
     * @return the doordinate in decimal format (longitude / latitude)
     */
    public double dmsToDecCoordinate(String strDegree, String strMinutes, String strSeconds)
    {
        double degree = 0;
        if(!StringUtils.nullOrWhitespace(strDegree)) degree = Double.valueOf(strDegree);

        double minutes = 0;
        if(!StringUtils.nullOrWhitespace(strMinutes)) minutes = Double.valueOf(strMinutes);

        double seconds = 0;
        if(!StringUtils.nullOrWhitespace(strSeconds)) seconds = Double.valueOf(strSeconds);


        return degree + (((minutes * 60) + (seconds)) / 3600);
    }



    public MultiValueHashMap<String, String> getPageTitle2Redirects(InputStream sWikipediaDump) throws FileNotFoundException, XMLStreamException
    {
        // <text xml:space="preserve">#REDIRECT [[Autopoiesis]]</text>
        // <text xml:space="preserve">#REDIRECT:[[Hans Leo Haßler]]</text>
        // <text xml:space="preserve">#redirect [[Weißer Hai]]</text>
        // #weiterleitung
        // <page>
        // <title>Autopoiesis</title>

        Logger.getLogger(WikipediaDumpParser.class.getName()).info("will collect redirects from wikipedia dump...");

        MultiValueHashMap<String, String> hsPageTitle2Redirects = new MultiValueBalancedTreeMap<String, String>();

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
                if(iTitlesRead % 200000 == 0) Logger.getLogger(WikipediaDumpParser.class.getName()).info("read doc #" + StringUtils.beautifyNumber(iTitlesRead));

                continue;
            }

            if(!xmlEvent.asStartElement().getName().getLocalPart().equals("text")) continue;

            // jetzt haben wir ein text-tag. Wir schauen, ob jetzt ein redirect kommt
            // entweder kommt ein charEvent oder ein EndEvent. Leere Texte gibts wohl auch
            XMLEvent nextEvent = xmlEventReader.peek();

            if(!nextEvent.isCharacters()) continue;

            String strCharEventData = readNextCharEventsText(xmlEventReader);
            if(strCharEventData == null) continue;


            strCharEventData = strCharEventData.trim();

            boolean bRedirect = false;

            if(strCharEventData.length() >= 9 && strCharEventData.substring(0, 9).equalsIgnoreCase("#redirect")) bRedirect = true;
            if(!bRedirect && strCharEventData.length() >= 8 && strCharEventData.substring(0, 8).equalsIgnoreCase("redirect") && !strCharEventData.contains("\n"))
                bRedirect = true;
            if(!bRedirect && strCharEventData.length() >= 14 && strCharEventData.substring(0, 14).equalsIgnoreCase("#weiterleitung")) bRedirect = true;
            if(!bRedirect && strCharEventData.length() >= 13 && strCharEventData.substring(0, 13).equalsIgnoreCase("weiterleitung") && !strCharEventData.contains("\n"))
                bRedirect = true;

            if(!bRedirect) continue;

            // wir haben einen redirect - der wird in unsere Datenstruktur eingetragen
            int iStart = strCharEventData.indexOf("[[");
            int iEnd = strCharEventData.indexOf("]]");
            if(iStart < 0 || iEnd < 0) continue;
            if(iEnd <= iStart) continue;
            if((iStart + 2) > strCharEventData.length() || iEnd > strCharEventData.length()) continue;




            String strRedirectTarget = strCharEventData.substring(iStart + 2, iEnd).trim();
            hsPageTitle2Redirects.add(strRedirectTarget, strCurrentTitle);



            // if("Venceslav Konstantinov".equalsIgnoreCase(strCurrentTitle) || "Venceslav Konstantinov".equalsIgnoreCase(strRedirectTarget))
            // System.out.println("redirect found: (" + hsPageTitle2Redirects.keySize() + ") " + strCurrentTitle + " => '" + strRedirectTarget + "'");

        }



        Logger.getLogger(WikipediaDumpParser.class.getName()).info("Redirects found: " + StringUtils.beautifyNumber(hsPageTitle2Redirects.valueSize()));



        return hsPageTitle2Redirects;

    }




    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context)
    {
        return Collections.singleton(MediaType.application("wikipedia+xml"));
    }



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException, TikaException
    {

        try
        {

            // wir iterieren schön über die page-Einträge. Darin gibt es dann title, timestamp, <contributor> => <username> und text. den text müssen
            // wir noch bereinigen. dazu nehmen wir eine Vorverarbeitung mit bliki - dazu müssen wir aber selbst nochmal den String vorbereiten und
            // nachbereinigen. Leider.

            WikipediaDumpParserConfig wikipediaDumpParserConfig = context.get(WikipediaDumpParserConfig.class);

            if(wikipediaDumpParserConfig == null)
            {
                Logger.getLogger(WikipediaDumpParser.class.getName()).info("No wikipedia parser config found. Will take the default one.");
                wikipediaDumpParserConfig = new WikipediaDumpParserConfig();
            }


            TikaInputStream tikaStream = TikaInputStream.get(stream);


            File fWikipediaDumpFile4Stream = tikaStream.getFile();

            MultiValueHashMap<String, String> hsPageTitle2Redirects = new MultiValueHashMap<String, String>();
            if(wikipediaDumpParserConfig.determinePageRedirects) hsPageTitle2Redirects = getPageTitle2Redirects(new FileInputStream(fWikipediaDumpFile4Stream));


            HashSet<String> hsRedirectPageTitles = new HashSet<String>(hsPageTitle2Redirects.values());

            String strCleanedText = "";
            String strBaseURL = null;


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

                    if(strCurrentTitle.equalsIgnoreCase("DuckDuckGo"))
                    {
                        int fasd = 8;
                    }

                    if(strCurrentTitle.toLowerCase().contains("duck") && strCurrentTitle.toLowerCase().contains("go"))
                    {
                        int is = 666;
                    }


                    // wenn der Titel eine redirect-Page ist, dann tragen wir die ganze Page aus der EventQueue aus, springen an das endPage, und
                    // haben somit diese Seite ignoriert. Ferner ignorieren wir auch spezielle wikipedia-Seiten
                    String strSmallTitle = strCurrentTitle.trim().toLowerCase();
                    if(hsRedirectPageTitles.contains(strCurrentTitle) || hsRedirectPageTitles.contains(strSmallTitle)
                            || hsRedirectPageTitles.contains(strCurrentTitle.trim()) || strSmallTitle.startsWith("category:") || strSmallTitle.startsWith("kategorie:")
                            || strSmallTitle.startsWith("vorlage:") || strSmallTitle.startsWith("template:") || strSmallTitle.startsWith("hilfe:")
                            || strSmallTitle.startsWith("help:") || strSmallTitle.startsWith("wikipedia:") || strSmallTitle.startsWith("portal:")
                            || strSmallTitle.startsWith("mediawiki:"))
                    {

                        while (true)
                        {
                            XMLEvent nextXmlEvent = xmlEventReader.nextEvent();
                            if(nextXmlEvent.isEndElement() && nextXmlEvent.asEndElement().getName().getLocalPart().equals("page")) break;
                        }
                    }
                    else
                    {
                        metadata.add(Metadata.TITLE, strCurrentTitle);
                        metadata.add(Metadata.SOURCE, strBaseURL + strCurrentTitle);


                        for (String strRedirect : hsPageTitle2Redirects.get(strCurrentTitle))
                        {
                            // wir ignorieren Titel, die sich lediglich durch groß/kleinschreibung unterscheiden
                            if(!StringUtils.containsIgnoreCase(strRedirect, metadata.getValues(Metadata.TITLE))) metadata.add(Metadata.TITLE, strRedirect);
                        }
                    }


                    continue;
                }




                // ##### der text
                if(xmlEvent.asStartElement().getName().getLocalPart().equals("text"))
                {
                    String strText = readNextCharEventsText(xmlEventReader);

                    if(wikipediaDumpParserConfig.parseLinksAndCategories) parseLinksAndCategories(strText, strBaseURL, metadata, handler);
                    if(wikipediaDumpParserConfig.parseInfoBoxes) parseInfoBox(strText, metadata, handler);
                    if(wikipediaDumpParserConfig.parseGeoCoordinates) parseGeoCoordinates(strText, metadata);

                    // aufgrund einiger Defizite in dem verwendeten cleaner müssen wir hier leider noch zu-und nacharbeiten
                    strText = strText.replaceAll("==\n", "==\n\n");
                    strText = strText.replaceAll("\n==", "\n\n==");


                    strCleanedText = m_wikiModel.render(new PlainTextConverter(), strText);

                    strCleanedText = strCleanedText.replaceAll("\\{\\{", " ");
                    strCleanedText = strCleanedText.replaceAll("\\}\\}", " ");

                    strCleanedText = StringEscapeUtils.unescapeHtml4(strCleanedText);

                    continue;
                }



                // ##### der timestamp
                if(xmlEvent.asStartElement().getName().getLocalPart().equals("timestamp"))
                {
                    String strTimestamp = readNextCharEventsText(xmlEventReader);

                    metadata.add(Metadata.MODIFIED, strTimestamp);

                    continue;
                }



                // ##### der username
                if(xmlEvent.asStartElement().getName().getLocalPart().equals("username"))
                {
                    String strUsername = readNextCharEventsText(xmlEventReader);

                    metadata.add(Metadata.CREATOR, strUsername);

                    continue;
                }







            }

        }
        catch (Exception e)
        {
            Logger.getLogger(WikipediaDumpParser.class.getName()).log(Level.SEVERE, "Error", e);
        }





    }



    protected void parseGeoCoordinates(String strText, Metadata metadata)
    {
        Matcher matcher = Pattern.compile("(?s)\\{\\{Coordinate (.*?)\\}\\}").matcher(strText);

        coord: while (matcher.find())
        {
            String strCoordinates = matcher.group(1);

            String[] straCoordinates = strCoordinates.split("\\|");



            for (String strPiece : straCoordinates)
            {
                if(strPiece.contains("text=")) continue coord;

                Matcher degreeMatcher = dmsCoordinatePattern.matcher(strPiece);
                if(degreeMatcher.find())
                {
                    // wir haben eine Angabe in DMS
                    double dInDezimal = dmsToDecCoordinate(degreeMatcher.group(1), degreeMatcher.group(2), degreeMatcher.group(3));

                    if("E".equals(degreeMatcher.group(4))) metadata.add("longitude", String.valueOf(dInDezimal));
                    if("W".equals(degreeMatcher.group(4))) metadata.add("longitude", String.valueOf(dInDezimal * -1));
                    if("N".equals(degreeMatcher.group(4))) metadata.add("latitude", String.valueOf(dInDezimal));
                    if("S".equals(degreeMatcher.group(4))) metadata.add("latitude", String.valueOf(dInDezimal * -1));

                }
                else
                {
                    // wir haben eine Angabe in dezimal - wenn alles stimmt
                    if(strPiece.contains("EW=") || strPiece.contains("NS="))
                    {
                        String strAttValue = strPiece.substring(3).trim();
                        try
                        {
                            Double.valueOf(strAttValue);

                            if(strPiece.contains("EW=")) metadata.add("longitude", strAttValue);
                            if(strPiece.contains("NS=")) metadata.add("latitude", strAttValue);
                        }
                        catch (NumberFormatException e)
                        {
                            // NOP - the geo coordinate entry is broken
                        }
                    }

                }
            }

        }

    }





    protected void parseInfoBox(String strText, Metadata metadata, ContentHandler handler) throws SAXException
    {

        // att-value paare mit | getrennt. Innerhalb eines values gibt es auch Zeilenumbrüche (mit '<br />') - dies gilt als Aufzählung
        // |Single1 |Datum1 , Besetzung1a Besetzung1b, Sonstiges1Titel |Sonstiges1Inhalt , Coverversion3 |Jahr3
        // | 1Option = 3
        // | 1Option Name = Demos
        // | 1Option Link = Demos
        // | 1Option Color =



        // als erstes schneiden wir mal die Infobox raus. (?m) ist multiline und (?s) ist dotall ('.' matcht auch line breaks)
        int iStartInfoBox = -1;
        int iEndInfoBox = -1;
        MatchResult infoMatch = StringUtils.findFirst("\\{\\{\\s*Infobox", strText);
        if(infoMatch != null)
        {
            iStartInfoBox = infoMatch.start();
            iEndInfoBox = StringUtils.findMatchingBracket(iStartInfoBox, strText) + 1;
        }
        else
            return;


        if(strText.length() < 3 || strText.length() < iEndInfoBox || iEndInfoBox <= 0 || (iStartInfoBox + 2) > iEndInfoBox) return;

        String strInfoBox = "";

        strInfoBox = strText.substring(iStartInfoBox + 2, iEndInfoBox);
        if(strInfoBox.length() < 5) return;


        String strCleanedInfoBox = m_wikiModel.render(new PlainTextConverter(), strInfoBox.replaceAll("<br />", "&lt;br /&gt;"));

        // da wir hier eigentlich relationierte Datensätze haben, machen wir auch einzelne, separierte Dokumente draus

        // System.out.println(strCleanedInfoBox);
        // System.out.println(strCleanedInfoBox.substring(0, strCleanedInfoBox.indexOf("\n")).trim());

        // erste Zeile bezeichnet die InfoBox
        int iIndex = strCleanedInfoBox.indexOf("|");
        if(iIndex == -1) iIndex = strCleanedInfoBox.indexOf("\n");
        if(iIndex == -1) return;
        String strInfoBoxName = strCleanedInfoBox.substring(7, iIndex).trim();
        metadata.add(infobox, strInfoBoxName);


        String[] straCleanedInfoBoxSplit = strCleanedInfoBox.split("\\s*\\|\\s*");

        HashMap<String, MultiValueHashMap<String, String>> hsSubDocId2AttValuePairsOfSubDoc = new HashMap<String, MultiValueHashMap<String, String>>();

        for (String strAttValuePair : straCleanedInfoBoxSplit)
        {

            // System.out.println("\nattValPair unsplittet " + strAttValuePair);
            // die Dinger sind mit einem '=' getrennt
            String[] straAtt2Value = strAttValuePair.split("=");

            if(straAtt2Value.length == 0 || straAtt2Value[0] == null) continue;
            if(straAtt2Value.length < 2 || straAtt2Value[1] == null) continue;

            String strAttName = straAtt2Value[0].trim();
            String strAttValues = straAtt2Value[1];
            if(StringUtils.nullOrWhitespace(strAttValues)) continue;
            // Innerhalb eines values gibt es auch Zeilenumbrüche (mit '<br />' bzw. '&lt;br /&gt;') - dies gilt als Aufzählung
            String[] straAttValues = strAttValues.split(Pattern.quote("&lt;br /&gt;"));
            // XXX wir werfen zusatzangaben in Klammern erst mal weg - man könnte sie auch als attnameAddInfo in einem extra Attribut speichern -
            // allerdings muß man dann wieder aufpassen, ob nicht ein subDocument entstehen muß (Bsp. mehrere Genre-entries mit jeweiliger
            // Jahreszahl)


            // der Attributname entscheidet nun, ob ein Dokument ausgelagert werden soll oder nicht. Ist darin eine Zahl enthalten, dann entfernen
            // wir diese und gruppieren alle att-value-paare mit dieser Zahl in einen extra Datensatz (MultiValueHashMap)
            Matcher numberMatcher = Pattern.compile("([\\D]*)(\\d+)([\\D]*)").matcher(strAttName);

            if(!numberMatcher.find())
            {
                // wir haben keine Zahl im AttNamen - wir tragen diesen Wert einfach in die Metadaten ein.
                for (String strAttValue : straAttValues)
                {
                    String strCleanedAttValue = cleanAttValue(strAttName, strAttValue);
                    if(strCleanedAttValue != null) metadata.add(strAttName, strCleanedAttValue);
                }
            }
            else
            {
                // wir haben eine Zahl im Namen - wir tragen den Wert in einem SubDocument unter der Id <zahl> ein
                String strPrefix = numberMatcher.group(1);
                String strNumber = numberMatcher.group(2);
                String strSuffix = numberMatcher.group(3);

                String strDataSetId = strPrefix + strNumber;
                String strFinalAttName = strPrefix + strSuffix;

                // wenn wir noch mehr Zahlen haben, dann haben wir geloost - und tragen es einfach ein
                if(numberMatcher.find())
                {
                    for (String strAttValue : straAttValues)
                    {
                        String strCleanedAttValue = cleanAttValue(strFinalAttName, strAttValue);
                        if(strCleanedAttValue != null) metadata.add(strFinalAttName, strCleanedAttValue);
                    }
                }

                // System.out.println("prefix " + strPrefix);
                // System.out.println("num " + strDataSetId);
                // System.out.println("suffix " + strSuffix);
                MultiValueHashMap<String, String> hsAttname2ValueOfSubDoc = hsSubDocId2AttValuePairsOfSubDoc.get(strDataSetId);
                if(hsAttname2ValueOfSubDoc == null)
                {
                    hsAttname2ValueOfSubDoc = new MultiValueHashMap<String, String>();
                    hsSubDocId2AttValuePairsOfSubDoc.put(strDataSetId, hsAttname2ValueOfSubDoc);
                }


                for (String strAttValue : straAttValues)
                    hsAttname2ValueOfSubDoc.add(strFinalAttName, strAttValue.replaceAll("\\(.*?\\)", "").trim());

            }
        }


        String strPageId = new UID().toString();
        metadata.add(LeechMetadata.id, strPageId);


        // we have to use the same metadata Object
        Metadata metadataBackup4ParentPage = TikaUtils.copyMetadata(metadata);

        for (MultiValueHashMap<String, String> hsAttValuePairsOfSubDoc : hsSubDocId2AttValuePairsOfSubDoc.values())
        {

            TikaUtils.clearMetadata(metadata);


            // die Referenz zu meinem parent
            metadata.add(LeechMetadata.parentId, strPageId);
            metadata.add(infobox, strInfoBoxName);
            String strChildId = new UID().toString();
            metadata.add(LeechMetadata.id, strChildId);
            // zum rückreferenzieren geben wir dem parent auch noch unsere id
            metadataBackup4ParentPage.add(LeechMetadata.childId, strChildId);


            for (Entry<String, String> attName2Value4SubDoc : hsAttValuePairsOfSubDoc.entryList())
            {
                String strAttName = attName2Value4SubDoc.getKey();
                String strAttValue = attName2Value4SubDoc.getValue();

                String strCleanedAttValue = cleanAttValue(strAttName, strAttValue);
                if(strCleanedAttValue != null) metadata.add(strAttName, strCleanedAttValue);
            }


            metadata.add(Metadata.CONTENT_TYPE, "application/wikipedia-meta+xml");

            // so erreichen wir, daß im übergeordneten ContentHandler mehrere Docs ankommen :)
            XHTMLContentHandler xhtml = new XHTMLContentHandler(handler, metadata);
            xhtml.startDocument();
            xhtml.endDocument();

        }

        TikaUtils.clearMetadata(metadata);
        TikaUtils.copyMetadataFromTo(metadataBackup4ParentPage, metadata);





    }



    protected void parseLinksAndCategories(String strText, String strBaseURL, Metadata metadata, ContentHandler handler) throws SAXException
    {
        // wir parsen jetzt auch noch die Infoboxen, und tragen auch die Kategorien mit in die Metadaten ein. Kinga fände auch noch die
        // Links aus dem Text ganz hübsch
        // Syntax:
        // interne links: [[Zielartikel|alternativer Text]] (alternativer text optional) es gibt auch noch den hop mit der raute:[[Titel#Überschrift
        // des Abschnitts]], wenn vor der Raute nix steht, dann wird innerhalb der Seite referenziert

        // externe links: [http://wissen.dradio.de/index.40.de.html?dram:article_id=6990 "Porträt" über Alan Smithee, DRadio Wissen] Trennung durch
        // Leerzeichen

        // Kategorien: [[Category:Category name|Sortkey]] [[Kategorie:Xyz]]

        // Infoboxen:
        // http://de.wikipedia.org/wiki/Vorlage:Infobox_Festival
        // http://de.wikipedia.org/wiki/Kategorie:Vorlage:Infobox_Musik


        // [ nicht gefolgt von [ (beliebige Zeichen non-greedy) gefolgt von ]
        // String strRegExp = "\\[(?!\\[)(.*?)\\]";
        String strRegExp = "\\[(.*?)\\]";


        HashSet<String> hsInternalLinks = new HashSet<String>();
        HashSet<String> hsExternalLinks = new HashSet<String>();

        Matcher matcher = Pattern.compile(strRegExp).matcher(strText);
        while (matcher.find())
        {
            String strMatch = matcher.group(1);


            // die mit vorangestelltem '[' sind die internen links, sowie die Kategorien. Die ohne sind die externen links.
            if(strMatch.startsWith("["))
            {
                // wir haben einen internen link oder eine Kategorie
                String strLinkOrCategory = strMatch.substring(1);
                int iIndex = strLinkOrCategory.indexOf("|");
                if(iIndex != -1) strLinkOrCategory = strLinkOrCategory.substring(0, iIndex);
                iIndex = strLinkOrCategory.indexOf(":");
                if(iIndex == -1)
                {
                    // wir haben einen internen link - wenn wir keine vorangestellte Raute haben, dann übernehmen wir ihn. Wenn wir eine Raute in der
                    // Mitte haben, dann übernehmen wir lediglich den ersten Teil
                    iIndex = strLinkOrCategory.indexOf("#");
                    if(iIndex == 0)
                        continue;
                    else if(iIndex == -1)
                        hsInternalLinks.add(strBaseURL + strLinkOrCategory);
                    else
                        hsInternalLinks.add(strBaseURL + strLinkOrCategory.substring(0, iIndex));
                }
                else
                {
                    // wir haben eine Variable - wenn es eine Kategorie ist, dann übernehmen wir sie..oder wir übernehmen erst mal alle
                    String strAttName = strLinkOrCategory.substring(0, iIndex);
                    String strCleanedAttValue = cleanAttValue(strAttName, strLinkOrCategory.substring(iIndex + 1));

                    if(strCleanedAttValue != null) metadata.add(strAttName, strCleanedAttValue);
                }

            }
            else
            {
                // wir haben einen externen link - wir werfen das label weg
                String strExternalLink = strMatch;
                int iIndex = strMatch.indexOf(" ");
                if(iIndex != -1) strExternalLink = strMatch.substring(0, iIndex);

                hsExternalLinks.add(strExternalLink);
            }
        }

        for (String strInternalLink : hsInternalLinks)
            metadata.add(internalLink, strInternalLink);

        for (String strExternalLink : hsExternalLinks)
            metadata.add(externalLink, strExternalLink);


    }




}
