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

package de.dfki.km.leech.io;



import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.URLName;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

import ucar.nc2.util.net.URLStreamHandlerFactory;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;



/**
 * This class offers the generation of streams out of an URL reference, together with crawling-relevant metadata. This can be done in order to simply
 * support new types of protocols.<br>
 * <br>
 * How to implement and register your own URL Protocol:<br>
 * <li>Create inside your jar manifest under a folder '/META-INF/services' a file named 'de.dfki.km.leech.io.URLStreamProvider'. Each line inside this
 * text file names a {@link URLStreamProvider} class (e.g. de.dfki.km.leech.io.HttpURLStreamProvider).</li> <li>Implement the referenced class as a
 * subclass of {@link URLStreamProvider}</li>
 * 
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
abstract public class URLStreamProvider
{
    protected static boolean bFactoryRegistered = false;

    protected final static HashMap<String, URLStreamProvider> m_protocol2StreamProvider = new HashMap<String, URLStreamProvider>();

    final protected static ServiceLoader<URLStreamProvider> m_serviceLoader = ServiceLoader.load(URLStreamProvider.class);

    final public static URLStreamHandlerFactory theOneAndOnlyURLStreamHandlerFactory;

    static
    {
        theOneAndOnlyURLStreamHandlerFactory = new URLStreamHandlerFactory();

        registerProtocols();
    }



    /**
     * Gets the configured URLStreamProvider for a given URL
     * 
     * @param strUrl the URL with the protocol you want to have streams and preliminary metadata for
     * 
     * @return the configured StreamProvider for the protocol of the given URL
     * 
     * @throws MalformedURLException
     */
    static public URLStreamProvider getURLStreamProvider(String strUrl) throws MalformedURLException
    {
        return m_protocol2StreamProvider.get(new URLName(strUrl).getProtocol());
    }



    /**
     * Gets the configured URLStreamProvider for a given URL
     * 
     * @param url the URL with the protocol you want to have streams and preliminary metadata for
     * 
     * @return the configured StreamProvider for the protocol of the given URL
     */
    static public URLStreamProvider getURLStreamProvider(URLName url)
    {
        return m_protocol2StreamProvider.get(url.getProtocol());
    }



    /**
     * Gets the configured URLStreamProvider for a given URL
     * 
     * @param url the URL with the protocol you want to have streams and preliminary metadata for
     * 
     * @return the configured StreamProvider for the protocol of the given URL
     */
    static public URLStreamProvider getURLStreamProvider(URL url)
    {
        return m_protocol2StreamProvider.get(url.getProtocol());
    }



    /**
     * Gets the configured URLStreamProvider for a given protocol (e.g. 'imap', 'imaps', 'http', 'file', etc.
     * 
     * @param strProtocol the protocol you want to have streams and preliminary metadata for
     * 
     * @return the configured StreamProvider for the protocol of the given URL
     */
    static public URLStreamProvider getURLStreamProvider4Protocol(String strProtocol)
    {
        return m_protocol2StreamProvider.get(strProtocol);
    }



    static public void registerProtocols()
    {

        // wir sorgen dafür, daß wir in der URL-Klasse neue Protokolle registrieren können

        try
        {
            URL.setURLStreamHandlerFactory(theOneAndOnlyURLStreamHandlerFactory);
            bFactoryRegistered = true;
        }
        catch (Error e)
        {
            if(!bFactoryRegistered)
            {
                Logger.getLogger(URLStreamProvider.class.getName())
                        .log(Level.SEVERE,
                                "The URLStreamHandlerFactory could not registered to the URL class. Ignore this message in the case you take care yet for stream creation on new protocols with the URL class.",
                                e);
            }
            else
                return;
        }

        // hier laden wir jetzt alle verfügbaren URLStreamProvider mit Hilfe dieses schicken java-mechanismus zum nachladen von Klassen über
        // einen manifesteintrag, so wie Tika das auch macht :). Dazu beschicken wir die Factory mit den Handlern, und merken uns hier noch das
        // mapping von protocol zum urlStreamProvider

        for (URLStreamProvider streamProvider : m_serviceLoader)
        {
            for (String strProtocol : streamProvider.getSupportedProtocols())
            {
                m_protocol2StreamProvider.put(strProtocol, streamProvider);
            }
        }
    }



    /**
     * Adds first metadata for the data entity behind a URL - data that is quickly available. This method is NOT to extract the content of the data
     * entity, this will be the job of the according Tika Parser, later in the crawling process. Here, some preliminary data such as modification time
     * or file name in a file-URL can be offered. Write inside what is there and what you want or need. The crawlers will forward everything to the
     * data handler afterwards.<br>
     * <br>
     * <b>IMPORTANT</b><br>
     * Leech and Tika needs some metadata entries in order to work - they have to be there in order to perform crawling and extracting content. These
     * are:<br>
     * {@link Metadata#RESOURCE_NAME_KEY}: this entry is performed by Tika for giving a stream a name. Will be used by some parsers. e.g. 'myFileName'<br>
     * {@link DublinCore#SOURCE}: this is the URL as String, needed by leech for referencing the data entity that should be crawled. e.g.
     * 'file:///home/dir/myFileName'<br>
     * {@link IncrementalCrawlingHistory#dataEntityId}: an identifier for a data entity that is independent from the content of this entity. It
     * is only for identifying the occurence, not to check whether it has changed (e.g. a filename). Needed by Leech in order to perform incremental
     * crawling.<br>
     * {@link IncrementalCrawlingHistory#dataEntityContentFingerprint} : some fingerprint/identifier that gives the hint whether the content of the
     * data entity has changed, e.g. the modified date of a file. Needed by Leech in order to perform incremental crawling.<br>
     * <br>
     * <b>IMPORTANT 2</b><br>
     * It is very good style to check whether there exists some metadata inside the parameter object, and only generate these entries NOT INSIDE YET.
     * Some crawlers can quickly fetch metadata information for a bag of data entities (during a recursive call), and are able to prefill the metadata
     * with this information even before this method invocation. You can save MUCH PERFORMANCE in such situations if you don't generate them again in
     * this method, for a single data entity.
     * 
     * @param url2getMetadata the url you want to get metadata from and add it to the given metadata object
     * 
     * @param metadata2fill the metadata object you potentially want to fill with first metadata. Can be null by convention, in this case the method
     *            returns a newly generated Metadata Object. Can be prefilled with known metadata, so don't generate it unnecessarily again in this
     *            method.
     * @param parseContext the parse Context configuration for the current crawl. May contain necessary, usefull context data
     * 
     * @return the parameter Metadata Object, potentially filled with some new metadata entries. A new Object in the case the parameter was null.
     *         Don't forget that most should be extracted from the parser implementations. Fill only what is necessary, what is not offered yet in the
     *         parameter Object - and low hanging fruits!
     * 
     * @throws Exception
     */
    abstract public Metadata addFirstMetadata(URLName url2getMetadata, Metadata metadata2fill, ParseContext parseContext) throws Exception;



    /**
     * Gets the stream to read out the content behind the given URL. Additional information to perform the connection can be specified in the parse
     * context.<br>
     * <br>
     * <b>IMPORTANT</b><br>
     * <li>Sometimes it is a very good idea that you wrap a stream inside {@link TikaInputStream} also with a {@link ShiftInitInputStream}. This will
     * prevent performance losts for initialisation in the case the stream won't be used at all.</li> <li>
     * Note that a stream will be initialized for sure for determining its mimetype (for reading the magic numbers). Thus the performance win with
     * {@link ShiftInitInputStream} is lost. Mimetype detection with the stream can be prevented by setting the correct Content-Type entry into the
     * metadata object, if you can. Examples: <code>metadata.set("Content-Type",
     * DatasourceMediaTypes.IMAPFOLDER.toString()) or metadata2fill.set("Content-Type", "message/rfc822");</code></li>
     * 
     * @param url2getStream the URL you want to have a stream from
     * @param metadata some preliminary metadata, as given from {@link URLStreamProvider#addFirstMetadata(URLName, Metadata, ParseContext)}
     * @param parseContext the parse Context configuration for the current crawl. May contain necessary, usefull context data
     * 
     * @return the stream for the data under this URL. This one is wrapped inside a {@link ShiftInitInputStream} Object to make sure that stream
     *         initialization will be only performed in the case the stream will be needed for extraction. It could be the case that Leech recognize
     *         because of the metadata that the data entity is crawled yet. In this case we don't want spend anything (internet connection inits,
     *         etc.) for stream object construction.
     * 
     * @throws Exception
     */
    abstract public TikaInputStream getStream(URLName url2getStream, Metadata metadata, ParseContext parseContext) throws Exception;



    /**
     * Gets the URL protocols supported by this URLStreamProvider. e.g. 'imap' and 'imaps' for an imap URL
     * (imap://uname:pwd@hostname:667/folder;uid=20).
     * 
     * @return the URL protocols supported by this URLStreamProvider
     */
    abstract public Set<String> getSupportedProtocols();



}
