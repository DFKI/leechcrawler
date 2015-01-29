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

package de.dfki.km.leech.config;



import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.exception.TikaException;
import org.apache.tika.mime.MediaTypeRegistry;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.Parser;

import de.dfki.km.leech.detect.LeechDefaultDetector;
import de.dfki.km.leech.parser.DirectoryCrawlerParser;
import de.dfki.km.leech.parser.HtmlCrawlerParser;
import de.dfki.km.leech.parser.ImapCrawlerParser;



/**
 * This is the default configuration for Leech. It sets the {@link LeechDefaultDetector} to detect some extra types as directories and adds additional
 * parsers as {@link DirectoryCrawlerParser}.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class LeechDefaultConfig extends TikaConfig
{

    static protected LeechDefaultConfig m_defaultLeechConfigSingleton;



    public static TikaConfig getDefaultLeechConfig()
    {

        try
        {

            if(m_defaultLeechConfigSingleton == null) m_defaultLeechConfigSingleton = new LeechDefaultConfig();

            return m_defaultLeechConfigSingleton;

        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to read default leech configuration", e);
        }
        catch (TikaException e)
        {
            throw new RuntimeException("Unable to access default leech configuration", e);
        }
    }


    protected Detector m_detector;



    protected CompositeParser m_parser;


    private LinkedList<Logger> m_llPdfBoxLogger = new LinkedList<>();



    public LeechDefaultConfig() throws TikaException, IOException
    {
        super();

        init();
    }



    @Override
    public Detector getDetector()
    {
        return m_detector;
    }



    @Override
    public MediaTypeRegistry getMediaTypeRegistry()
    {
        return super.getMediaTypeRegistry();
    }



    @Override
    public MimeTypes getMimeRepository()
    {
        return super.getMimeRepository();
    }



    @Override
    public Parser getParser()
    {
        return m_parser;
    }



    protected void init()
    {
        LinkedList<Parser> llParsers = new LinkedList<Parser>();


        // der default-Parser aus der TikaConfig
        llParsers.add(super.getParser());

        // die Leech-datasource-crawler-parser - die letzten werden priorisiert, somit können wir hier z.b. den Original-html-parser überschreiben
        llParsers.add(new DirectoryCrawlerParser());
        llParsers.add(new HtmlCrawlerParser());
        llParsers.add(new ImapCrawlerParser());



        m_parser = new CompositeParser(this.getMediaTypeRegistry(), llParsers);
        m_detector = new LeechDefaultDetector(m_parser);

        
        // die kommen in ein field, da die Einstellung wohl nur so lange gültig ist, wie es noch eine gültige Referenz zu diesen Objekten gibt
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.util.PDFStreamEngine"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.encoding.Encoding"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.pdfparser.BaseParser"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.pdmodel.font.PDSimpleFont"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.pdfparser.XrefTrailerResolver"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.filter.FlateFilter"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.pdfparser.PDFParser"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.util.operator.SetTextFont"));
        m_llPdfBoxLogger.add(Logger.getLogger("org.apache.pdfbox.*"));
        
        
        
        
        
        for(Logger logger : m_llPdfBoxLogger)
            logger.setLevel(Level.OFF);

    }







}
