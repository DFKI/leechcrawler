/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech;



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.URLName;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.config.DirectoryCrawlerContext;
import de.dfki.km.leech.config.LeechDefaultConfig;
import de.dfki.km.leech.io.URLStreamProvider;
import de.dfki.km.leech.parser.DirectoryCrawlerParser;
import de.dfki.km.leech.parser.filter.URLFilteringParser;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingParser;
import de.dfki.km.leech.sax.DataSinkContentHandler;
import de.dfki.km.leech.sax.PrintlnContentHandler;
import de.dfki.km.leech.util.ExceptionUtils;
import de.dfki.km.leech.util.StringUtils;
import de.dfki.km.leech.util.UrlUtil;



/**
 * This is the main class, the entry point. Feel free to select one of the plenty of parse-methods<br>
 * <br>
 * Crawling in Leech will be performed by using a ContentHandler, that is invoked for every data entity that is recognized during the crawl. Leech
 * offers a special {@link DataSinkContentHandler} with abstract methods you can implement to store the data into your data store, e.g. a Lucene
 * index. By using {@link DataSinkContentHandler}, it is easy for you to perfom incremental indexing, which means that during a crawl, Leech remarks
 * which data entities were crawled, and at subsequent crawls only those data entities that are new or have changed will be parsed again. Further, you
 * will get the information which data entities were removed since the last crawl. <br>
 * To configure a crawl, there exists several **Context classes for passing into the ParseContext. E.g. a CrawlerContext Object will be used for all
 * configuration parameters that are common for all types of possible datasources. There you can pass things like crawling depth or the path to an
 * incremental crawling history. You also can request stopping a running crawling process.<br>
 * There exists some more specialised **Context classes to adjust the crawling of specific data sources. For example, {@link DirectoryCrawlerContext}
 * let you choose whether symbolic links should be followed or not during crawling a file system. Look into the package de.dfki.km.leech.config for
 * all Context classes offered by Leech.<br>
 * <br>
 * Now some examples as a starting point.<br>
 * To enable incremental indexing during a crawl, pass a {@link CrawlerContext} instance with a path to the history into the ParseContext parameter of
 * a Leech.parse(..) method. Leech will create a new history in the case there is no existing one under the given path. {@link PrintlnContentHandler}
 * is an implementation of {@link DataSinkContentHandler} which simply writes all content inclusive metadata to stout. :<br>
 * <br>
 * <code>
 * Leech leech = new Leech();<br>
 * CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");<br>
 * leech.parse(new File("resource"), new PrintlnContentHandler(), crawlerContext.createParseContext());<br>
 * </code> <br>
 * To request stopping a crawl (should be invoked in a different thread than leech.parse(..):<br>
 * <br>
 * <code>
 * crawlerContext.requestStop()
 * </code>
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */



public class Leech extends Tika
{



    public Leech()
    {
        super(LeechDefaultConfig.getDefaultLeechConfig());
    }



    @Override
    public String detect(File file) throws IOException
    {
        return detect(new URLName(file.toURI().toURL()));
    }




    @Override
    public String detect(URL url)
    {
        throw new UnsupportedOperationException(
                "The java.net.URL class methods are not supported because our mechanism supporting new protocols and the according stream creation differ.\n"
                        + "Use the according URLName method instead");
    }



    public String detect(URLName url) throws IOException
    {
        InputStream stream = null;

        try
        {

            Metadata metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, null, null);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, null);


            return detect(stream, metadata);

        }
        catch (Exception e)
        {
            Logger.getLogger(Leech.class.getName()).log(Level.SEVERE, "Error", e);

            return null;
        }
        finally
        {
            if(stream != null) stream.close();
        }
    }



    protected ContentHandler getContentHandler(ParseContext context)
    {

        CrawlerContext crawlerContext = context.get(CrawlerContext.class);

        if(crawlerContext == null)
            throw new IllegalStateException(
                    "no crawlerContext was set. Set a CrawlerContext with a configured handler or use another method with directly specifying a handler.");


        ContentHandler handler2use4recursiveCall = crawlerContext.getContentHandler();

        if(!StringUtils.nullOrWhitespace(crawlerContext.getContentHandlerClassName()))
            try
            {
                handler2use4recursiveCall = (ContentHandler) Class.forName(crawlerContext.getContentHandlerClassName()).newInstance();
            }
            catch (Exception e)
            {
                Logger.getLogger(DirectoryCrawlerParser.class.getName()).log(Level.SEVERE,
                        "Error during the instantiation of the configured content handler " + crawlerContext.getContentHandlerClassName(), e);
            }


        if(handler2use4recursiveCall == null)
            throw new IllegalStateException("no contentHandler was set. Have a look into the class CrawlerContext");


        return handler2use4recursiveCall;
    }







    @Override
    public Parser getParser()
    {
        return new URLFilteringParser(new IncrementalCrawlingParser(super.getParser()));
    }



    @Override
    public Reader parse(File file) throws IOException
    {
        return parse(new URLName(file.toURI().toURL()));
    }



    /**
     * Parse a directory or a file with a callback-contenthandler. We recommend to use an own implementation of DataSinkContentHandler. In the case
     * you want to use another ContentHandler, be aware that this Object is re-used at every recursive invocation. So make sure that this is possible,
     * and all internal members (e.g. writers, etc.) are re-initialized at the new invocation (maybe clear them inside endDocument(), or inside
     * startDocument()). In the case the handler does not have any internal states that are critical, there should be no problems at all. In the case
     * you have a critical handler with a default constructor, you can also set the class name inside the CrawlerContext object inside ParseContext.
     * In this case, a new handler object will be created at every recursive call..
     * 
     * @param file the file you want to crawl/extract content from
     * @param handler the handler that should handle the extracted data
     * 
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(File file, ContentHandler handler) throws IOException, SAXException, TikaException
    {
        ParseContext context = new ParseContext();
        context.set(Parser.class, super.getParser());

        context.set(CrawlerContext.class, new CrawlerContext().setContentHandler(handler));

        Metadata metadata = new Metadata();
        InputStream stream = null;

        if(handler instanceof DataSinkContentHandler) metadata = ((DataSinkContentHandler) handler).getMetaData();

        try
        {
            URLName url = new URLName(file.toURI().toURL());

            metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);

            getParser().parse(stream, handler, metadata, context);

        }
        catch (Exception e)
        {
            ExceptionUtils.handleException(e, null, metadata, context.get(CrawlerContext.class), context, 0, handler);
        }
        finally
        {
            if(stream != null) stream.close();
        }
    }



    /**
     * Parse a directory or a file with a callback-contenthandler. We recommend to use an own implementation of DataSinkContentHandler. In the case
     * you want to use another ContentHandler, be aware that this Object is re-used at every recursive invocation. So make sure that this is possible,
     * and all internal members (e.g. writers, etc.) are re-initialized at the new invocation (maybe clear them inside endDocument(), or inside
     * startDocument()). In the case the handler does not have any internal states that are critical, there should be no problems at all. In the case
     * you have a critical handler with a default constructor, you can also set the class name inside the CrawlerContext object inside ParseContext.
     * In this case, a new handler object will be created at every recursive call..
     * 
     * @param file the file you want to crawl/extract content from
     * @param handler the handler that should handle the extracted data
     * @param context the parsing context to use. An entry with the configured parser will be added by the method. You can pass in an CrawlerContext
     *            instance to e.g. set the contentHandler for recursive crawls or enable incremental crawling.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(File file, ContentHandler handler, ParseContext context) throws IOException, SAXException, TikaException
    {
        context.set(Parser.class, super.getParser());

        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        if(crawlerContext == null)
        {
            crawlerContext = new CrawlerContext();
            context.set(CrawlerContext.class, crawlerContext);
        }
        crawlerContext.setContentHandler(handler);

        Metadata metadata = new Metadata();
        InputStream stream = null;

        if(handler instanceof DataSinkContentHandler) metadata = ((DataSinkContentHandler) handler).getMetaData();

        try
        {
            URLName url = new URLName(file.toURI().toURL());

            metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);

            getParser().parse(stream, handler, metadata, context);

        }
        catch (Exception e)
        {
            ExceptionUtils.handleException(e, null, metadata, context.get(CrawlerContext.class), context, 0, handler);
        }
        finally
        {
            if(stream != null) stream.close();
        }
    }





    /**
     * Parse a directory or a file by specifying a ParseContext config. You can pass in an CrawlerContext instance to e.g. set the ContentHandler for
     * recursive crawls. This one will be newly instantiated with the default constructor for every recursive call. Alternatively, you can also set a
     * contentHandler object for reuse.
     * 
     * @param file the file you want to crawl/extract content from
     * @param context the parsing context to use. An entry with the configured parser will be added by the method. You can pass in an CrawlerContext
     *            instance to e.g. set the contentHandler for recursive crawls or enable incremental crawling.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(File file, ParseContext context) throws IOException, SAXException, TikaException
    {
        context.set(Parser.class, super.getParser());

        Metadata metadata = new Metadata();
        InputStream stream = null;

        ContentHandler handler = getContentHandler(context);
        if(handler instanceof DataSinkContentHandler) metadata = ((DataSinkContentHandler) handler).getMetaData();

        try
        {
            URLName url = new URLName(file.toURI().toURL());

            metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);


            getParser().parse(stream, handler, metadata, context);

        }
        catch (Exception e)
        {
            ExceptionUtils.handleException(e, null, metadata, new CrawlerContext(), context, 0, handler);
        }
        finally
        {
            if(stream != null) stream.close();
        }
    }







   


    /**
     * Parse a directory or a file with a callback-contenthandler. We recommend to use an own implementation of DataSinkContentHandler. In the case
     * you want to use another ContentHandler, be aware that this Object is re-used at every recursive invocation. So make sure that this is possible,
     * and all internal members (e.g. writers, etc.) are re-initialized at the new invocation (maybe clear them inside endDocument(), or inside
     * startDocument()). In the case the handler does not have any internal states that are critical, there should be no problems at all.In the case
     * you have a critical handler with a default constructor, you can also set the class name inside the CrawlerContext object inside ParseContext.
     * In this case, a new handler object will be created at every recursive call..
     * 
     * @param strSourceString the URL string you want to crawl/extract content from. This can ether be a file://, http:// or - in future - other urls
     *            (e.g. for databases, imap, webDAV, etc...). In the case the string is no correct url string, the method will use the string as file
     *            path and then generates an according URL
     * @param handler the handler that should handle the extracted data
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(String strSourceString, ContentHandler handler) throws IOException, SAXException, TikaException
    {
        parse(UrlUtil.sourceString2URL(strSourceString), handler);
    }



    /**
     * Parse a directory or a file with a callback-contenthandler. We recommend to use an own implementation of DataSinkContentHandler. In the case
     * you want to use another ContentHandler, be aware that this Object is re-used at every recursive invocation. So make sure that this is possible,
     * and all internal members (e.g. writers, etc.) are re-initialized at the new invocation (maybe clear them inside endDocument(), or inside
     * startDocument()). In the case the handler does not have any internal states that are critical, there should be no problems at all.In the case
     * you have a critical handler with a default constructor, you can also set the class name inside the CrawlerContext object inside ParseContext.
     * In this case, a new handler object will be created at every recursive call..
     * 
     * @param strSourceString the URL string you want to crawl/extract content from. This can ether be a file://, http:// or - in future - other urls
     *            (e.g. for databases, imap, webDAV, etc...). In the case the string is no correct url string, the method will use the string as file
     *            path and then generates an according URL
     * @param handler the handler that should handle the extracted data
     * @param context the parsing context to use. An entry with the configured parser will be added by the method. You can pass in an CrawlerContext
     *            instance to e.g. set the contentHandler for recursive crawls or enable incremental crawling.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(String strSourceString, ContentHandler handler, ParseContext context) throws IOException, SAXException, TikaException
    {
        parse(UrlUtil.sourceString2URL(strSourceString), handler, context);
    }



    /**
     * Parse an URL by specifying a ParseContext config. You can pass in an CrawlerContext instance to e.g. set the ContentHandler for recursive
     * crawls. This one will be newly instantiated with the default constructor for every recursive call.
     * 
     * @param strSourceString the URL string you want to crawl/extract content from. This can ether be a file://, http:// or - in future - other urls
     *            (e.g. for databases, imap, webDAV, etc...). In the case the string is no correct url string, the method will use the string as file
     *            path and then generates an according URL
     * @param context the parsing context to use. An entry with the configured parser will be added by the method. You can pass in an CrawlerContext
     *            instance to e.g. set the contentHandler for recursive crawls or enable incremental crawling.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(String strSourceString, ParseContext context) throws IOException, SAXException, TikaException
    {
        parse(UrlUtil.sourceString2URL(strSourceString), context);
    }



    @Override
    public Reader parse(URL url) throws IOException
    {
        throw new UnsupportedOperationException(
                "The java.net.URL class methods are not supported because our mechanism supporting new protocols and the according stream creation differ.\n"
                        + "Use the according URLName method instead");
    }



    public Reader parse(URLName url) throws IOException
    {
        url = UrlUtil.normalizeURL(url);


        InputStream stream;
        try
        {
            Metadata metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, null, null);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, null);


            return parse(stream, metadata);

        }
        catch (Exception e)
        {
            Logger.getLogger(Leech.class.getName()).log(Level.SEVERE, "Error", e);

            return null;
        }
    }



    /**
     * Parse a directory or a file with a callback-contenthandler. We recommend to use an own implementation of DataSinkContentHandler. In the case
     * you want to use another ContentHandler, be aware that this Object is re-used at every recursive invocation. So make sure that this is possible,
     * and all internal members (e.g. writers, etc.) are re-initialized at the new invocation (maybe clear them inside endDocument(), or inside
     * startDocument()). In the case the handler does not have any internal states that are critical, there should be no problems at all.In the case
     * you have a critical handler with a default constructor, you can also set the class name inside the CrawlerContext object inside ParseContext.
     * In this case, a new handler object will be created at every recursive call..
     * 
     * @param url the URL you want to crawl/extract content from. This can ether be a file://, http:// or - in future - other urls (e.g. for
     *            databases, imap, webDAV, etc...)
     * @param handler the handler that should handle the extracted data
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(URLName url, ContentHandler handler) throws IOException, SAXException, TikaException
    {
        url = UrlUtil.normalizeURL(url);

        ParseContext context = new ParseContext();
        context.set(Parser.class, super.getParser());

        context.set(CrawlerContext.class, new CrawlerContext().setContentHandler(handler));

        Metadata metadata = new Metadata();
        InputStream stream = null;

        if(handler instanceof DataSinkContentHandler) metadata = ((DataSinkContentHandler) handler).getMetaData();

        try
        {

            metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);

            getParser().parse(stream, handler, metadata, context);

        }
        catch (Exception e)
        {
            ExceptionUtils.handleException(e, null, metadata, context.get(CrawlerContext.class), context, 0, handler);
        }
        finally
        {
            if(stream != null) stream.close();
        }

    }



    /**
     * Parse a directory or a file with a callback-contenthandler. We recommend to use an own implementation of DataSinkContentHandler. In the case
     * you want to use another ContentHandler, be aware that this Object is re-used at every recursive invocation. So make sure that this is possible,
     * and all internal members (e.g. writers, etc.) are re-initialized at the new invocation (maybe clear them inside endDocument(), or inside
     * startDocument()). In the case the handler does not have any internal states that are critical, there should be no problems at all.In the case
     * you have a critical handler with a default constructor, you can also set the class name inside the CrawlerContext object inside ParseContext.
     * In this case, a new handler object will be created at every recursive call..
     * 
     * @param url the URL you want to crawl/extract content from. This can ether be a file://, http:// or - in future - other urls (e.g. for
     *            databases, imap, webDAV, etc...)
     * @param handler the handler that should handle the extracted data
     * @param context the parsing context to use. An entry with the configured parser will be added by the method. You can pass in an CrawlerContext
     *            instance to e.g. set the contentHandler for recursive crawls or enable incremental crawling.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(URLName url, ContentHandler handler, ParseContext context) throws IOException, SAXException, TikaException
    {
        url = UrlUtil.normalizeURL(url);

        context.set(Parser.class, super.getParser());

        CrawlerContext crawlerContext = context.get(CrawlerContext.class);
        if(crawlerContext == null)
        {
            crawlerContext = new CrawlerContext();
            context.set(CrawlerContext.class, crawlerContext);
        }
        crawlerContext.setContentHandler(handler);

        Metadata metadata = new Metadata();
        InputStream stream = null;

        if(handler instanceof DataSinkContentHandler) metadata = ((DataSinkContentHandler) handler).getMetaData();

        try
        {
            metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);

            getParser().parse(stream, handler, metadata, context);

        }
        catch (Exception e)
        {
            ExceptionUtils.handleException(e, null, metadata, context.get(CrawlerContext.class), context, 0, handler);
        }
        finally
        {
            if(stream != null) stream.close();
        }

    }



    /**
     * Parse an URL by specifying a ParseContext config. You can pass in an CrawlerContext instance to e.g. set the ContentHandler for recursive
     * crawls. This one will be newly instantiated with the default constructor for every recursive call.
     * 
     * @param url the URL you want to crawl/extract content from. This can ether be a file://, http:// or - in future - other urls (e.g. for
     *            databases, imap, webDAV, etc...)
     * @param context the parsing context to use. An entry with the configured parser will be added by the method. You can pass in an CrawlerContext
     *            instance to e.g. set the contentHandler for recursive crawls or enable incremental crawling.
     * 
     * @throws IOException
     * @throws SAXException
     * @throws TikaException
     */
    public void parse(URLName url, ParseContext context) throws IOException, SAXException, TikaException
    {
        url = UrlUtil.normalizeURL(url);

        context.set(Parser.class, super.getParser());

        Metadata metadata = new Metadata();
        InputStream stream = null;

        ContentHandler handler = getContentHandler(context);

        if(handler instanceof DataSinkContentHandler) metadata = ((DataSinkContentHandler) handler).getMetaData();

        try
        {
            metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);

            getParser().parse(stream, handler, metadata, context);

        }
        catch (Exception e)
        {
            ExceptionUtils.handleException(e, null, metadata, context.get(CrawlerContext.class), context, 0, handler);
        }
        finally
        {
            if(stream != null) stream.close();
        }

    }



    @Override
    public String parseToString(File file) throws IOException, TikaException
    {
        return parseToString(new URLName(file.toURI().toURL()));
    }



    @Override
    public String parseToString(URL url) throws IOException, TikaException
    {
        throw new UnsupportedOperationException(
                "The java.net.URL class methods are not supported because our mechanism supporting new protocols and the according stream creation differ.\n"
                        + "Use the according URLName method instead");
    }




    public String parseToString(URLName url) throws IOException, TikaException
    {
        url = UrlUtil.normalizeURL(url);

        InputStream stream = null;

        try
        {
            Metadata metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, null, null);
            stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, null);


            return parseToString(stream, metadata);
        }
        catch (Exception e)
        {
            throw new TikaException("Error while parsing " + url.getFile(), e);
        }
    }



    



}
