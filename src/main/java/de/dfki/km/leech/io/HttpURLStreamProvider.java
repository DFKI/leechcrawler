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



import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.metadata.LeechMetadata;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.util.CookieManager;
import de.dfki.km.leech.util.LeechException;
import de.dfki.km.leech.util.UrlUtil;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;

import javax.mail.URLName;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;



public class HttpURLStreamProvider extends URLStreamProvider
{

    protected static final int connectTimeout = 20000;

    protected static final int MAX_REDIRECTIONS = 20;

    protected static final int readTimeout = 20000;



    protected static String getRedirectedUrl(URL url, URLConnection connection) throws IOException
    {
        String newLocation = connection.getHeaderField("Location");
        if(newLocation == null)
            throw new IOException("missing redirection location");
        else
            return new URL(url, newLocation).toString();
    }





    protected static boolean isRedirected(int responseCode)
    {
        return responseCode == HttpURLConnection.HTTP_MULT_CHOICE || responseCode == HttpURLConnection.HTTP_MOVED_PERM
                || responseCode == HttpURLConnection.HTTP_MOVED_TEMP || responseCode == HttpURLConnection.HTTP_SEE_OTHER;
    }




    /**
     * Adds first metadata and metadata relevant for incremental indexing to the given metadata object
     * 
     * @param url2getMetadata the url for which metadata should be extracte
     * @param metadata2fill the metadata object. The method will put several entries, as Metadata.SOURCE, LeechMetadata.RESOURCE_NAME_KEY,
     *            Metadata.CONTENT_ENCODING, Metadata.CONTENT_TYPE, Metadata.CONTENT_LOCATION and, last but not least, the
     *            {@link IncrementalCrawlingHistory#dataEntityId} and {@link IncrementalCrawlingHistory#dataEntityContentFingerprint} to
     *            determine whether the content behind the url was modified since the last crawl or not. The URL path entry for Metadata.SOURCE is
     *            the last URL behind potential previous redirects (in the case its an http connection). The origin URL will be written into an
     *            attribute "originalsource" in the case it differs from the one into Metadata.SOURCE. To determine whether an url was modified or
     *            not, the method needs a configured crawling history.
     * @param parseContext the parsing context to specify a crawling history. Can be null, in this case no history will be used (of course ;) )
     * 
     * @return the metadata object, enriched with new metadata (in the case this metadata was not set yet)
     */
    @Override
    public Metadata addFirstMetadata(URLName url2getMetadata, Metadata metadata2fill, ParseContext parseContext) throws Exception
    {


        if(metadata2fill == null) metadata2fill = new Metadata();


        // wenn das Teil schon gefüllt ist, dann machen wir gar nix
        if(!(metadata2fill.get(Metadata.SOURCE) == null || metadata2fill.get(LeechMetadata.RESOURCE_NAME_KEY) == null
                || metadata2fill.get(Metadata.CONTENT_ENCODING) == null || metadata2fill.get(Metadata.CONTENT_TYPE) == null
                || metadata2fill.get(Metadata.CONTENT_LOCATION) == null
                || metadata2fill.get(IncrementalCrawlingHistory.dataEntityContentFingerprint) == null || metadata2fill
                    .get(IncrementalCrawlingHistory.dataEntityId) == null))
        {
            // alle sind bereits gesetzt
            return metadata2fill;
        }


        IncrementalCrawlingHistory crawlingHistory = null;

        if(parseContext == null) parseContext = new ParseContext();

        CrawlerContext crawlerContext = parseContext.get(CrawlerContext.class, new CrawlerContext());

        crawlingHistory = crawlerContext.getIncrementalCrawlingHistory();

        // müssen wir hier evtl. die Lucene-Teile auch wieder closen? das ist immerhin eine utility-Methode^^ och - wir haben ja auch noch nen
        // shutdown hook, und nach dem crawl wirds eh geschlossen. Klingt safe
        if(crawlingHistory != null) crawlingHistory.openDBStuff();


        // keep a backup of the originally passed url
        String strOriginalUrlString = url2getMetadata.toString();
        metadata2fill.set(Metadata.SOURCE, strOriginalUrlString);

        URLConnection connection = null;
        int nrRedirections = 0;

        String strCurrentUrl = url2getMetadata.toString();
        
        CookieManager cookies = crawlerContext.getCookieManager();
        
        // We're going to loop, accessing urls until we arrive at a url that is not redirected. The
        // redirection is followed manually rather than automatically, which is HttpURLConnection's
        // default behaviour, so that we know the actual url we arrive at.
        while (true)
        {
            // check if we haven't been redirected too often
            if(nrRedirections > MAX_REDIRECTIONS)
            {
                throw new IOException("too many redirections, max = " + MAX_REDIRECTIONS + ", url = " + strOriginalUrlString);
            }

            // normalize the URL
            URL currentUrl = new URL(strCurrentUrl);
            currentUrl = new URL(UrlUtil.normalizeURL(new URLName(currentUrl)).toString());
            strCurrentUrl = currentUrl.toExternalForm();

            // see if a date was registered for this url
            Date ifModifiedSinceDate = null;
            if(crawlingHistory != null)
            {
                String lastIfModifiedSinceDate = crawlingHistory.getDataEntityContentFingerprint(strCurrentUrl);
                if(lastIfModifiedSinceDate != null && lastIfModifiedSinceDate.matches("\\d+")) ifModifiedSinceDate = new Date(Long.valueOf(lastIfModifiedSinceDate));
            }

            try
            {
                // maybe there exists other connections as http - in this case we want to fall back zu standard Tika behaviour
                connection = currentUrl.openConnection();

                if(!(connection instanceof HttpURLConnection)) break;

                ((HttpURLConnection) connection).setRequestMethod("HEAD");
                cookies.setCookies(connection);
                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);
                connection.setRequestProperty("Accept-Encoding", "gzip");

                Map<String, String> userHeaders = crawlerContext.getUserHeaders();
                if (userHeaders != null) {
                    for (Map.Entry<String, String> entry : userHeaders.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                String userAgent = crawlerContext.getUserAgent();
                if (userAgent != null && !userAgent.isEmpty())
                {
                    connection.setRequestProperty("User-Agent", userAgent);
                }

                ((HttpURLConnection) connection).setInstanceFollowRedirects(false);
                if(ifModifiedSinceDate != null)
                {
                    connection.setIfModifiedSince(ifModifiedSinceDate.getTime());
                }

                // send the request to the server
                connection.connect();
                cookies.storeCookies(connection);
            }
            catch (Exception e)
            {
                // I've seen IllegalArgumentExceptions in the sun.net classes here because of some freaky URLs
                // that did not generate MalformedUrlExceptions, so therefore a "catch "Exception" to be sure
                if(e instanceof IOException)
                {
                    throw (IOException) e;
                }
                else
                {
                    throw new LeechException("connection to " + strOriginalUrlString + " resulted in an exception", e);
                }
            }

            // check for http-specific response codes
            int responseCode = ((HttpURLConnection) connection).getResponseCode();

            if(isRedirected(responseCode))
            {
                // follow the redirected url
                String lastUrl = strCurrentUrl;
                strCurrentUrl = getRedirectedUrl(currentUrl, connection);
                nrRedirections++;


                // check for urls that redirect to themselves
                if(strCurrentUrl.equals(lastUrl))
                {
                    throw new LeechException("url redirects to itself: " + strCurrentUrl);
                }
            }
            else if(responseCode == HttpURLConnection.HTTP_NOT_FOUND)
            {
                throw new LeechException(strCurrentUrl + " not found");
            }
            else if(responseCode == HttpURLConnection.HTTP_NOT_MODIFIED)
            {
                // des isch nicht modifiziert seit dem letzten crawl - wir geben die ('modification') time des letzten crawls zurück, damit des teil
                // als unmodifiziert erkannt wird.
                if(crawlingHistory != null && ifModifiedSinceDate != null)
                    metadata2fill.set(IncrementalCrawlingHistory.dataEntityContentFingerprint, String.valueOf(ifModifiedSinceDate.getTime()));

                break;
            }
            else if(responseCode != HttpURLConnection.HTTP_OK)
            {
                // this is a communication error, quit with an exception
                throw new IOException("Http connection error, response code = " + responseCode + ", url = " + currentUrl);
            }
            else
            {
                // we're done
                break;
            }
        }


        if(metadata2fill.get(IncrementalCrawlingHistory.dataEntityContentFingerprint) == null)
            metadata2fill.set(IncrementalCrawlingHistory.dataEntityContentFingerprint, String.valueOf(System.currentTimeMillis()));

        // die Einträge, die Tika auch in das metadata einträgt, und noch etwas dazu

        metadata2fill.set(LeechMetadata.RESOURCE_NAME_KEY, strCurrentUrl);

        metadata2fill.set(Metadata.SOURCE, strCurrentUrl);
        metadata2fill.set(IncrementalCrawlingHistory.dataEntityId, strCurrentUrl);

        if(strOriginalUrlString.indexOf(strCurrentUrl) == -1) metadata2fill.set("originalsource", strOriginalUrlString);



        String type = connection.getContentType();
        //text/xml is far too general to select the right parser
        if(type != null && !type.contains("text/xml")) metadata2fill.set(Metadata.CONTENT_TYPE, type);


        String encoding = connection.getContentEncoding();
        if(encoding != null) metadata2fill.set(Metadata.CONTENT_ENCODING, encoding);


        int length = connection.getContentLength();
        if(length >= 0) metadata2fill.set(Metadata.CONTENT_LENGTH, Integer.toString(length));

        // das brauchen wir noch, um relative links aufzulösen
        metadata2fill.set(Metadata.CONTENT_LOCATION, strCurrentUrl);



        return metadata2fill;
    }




    @Override
    public TikaInputStream getStream(URLName url2getStream, Metadata metadata, ParseContext parseContext) throws Exception
    {
        final URL asUrl = new URL(url2getStream.toString());
        final CrawlerContext crawlerContext = parseContext.get(CrawlerContext.class, new CrawlerContext());

        return TikaInputStream.get(new ShiftInitInputStream()
        {
            @Override
            protected InputStream initBeforeFirstStreamDataAccess() throws Exception
            {
                CookieManager cookies = crawlerContext.getCookieManager();
                
                URLConnection connection = asUrl.openConnection();
                cookies.setCookies(connection);

                connection.setConnectTimeout(connectTimeout);
                connection.setReadTimeout(readTimeout);
                connection.setRequestProperty("Accept-Encoding", "gzip");

                Map<String, String> userHeaders = crawlerContext.getUserHeaders();
                if (userHeaders != null) {
                    for (Map.Entry<String, String> entry : userHeaders.entrySet()) {
                        connection.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }

                String userAgent = crawlerContext.getUserAgent();
                if (userAgent != null && !userAgent.isEmpty())
                {
                    connection.setRequestProperty("User-Agent", userAgent);
                }

                connection.connect();
                cookies.storeCookies(connection);
                InputStream ourStream = connection.getInputStream();

                String strContentEncoding = connection.getHeaderField("Content-Encoding");
                if(strContentEncoding != null) strContentEncoding = strContentEncoding.toLowerCase().trim();


                if("gzip".equals(strContentEncoding))
                    ourStream = new BufferedInputStream(new GZIPInputStream(ourStream));
                else
                    ourStream = new BufferedInputStream(ourStream);

                return ourStream;
            }
        });
    }







    @Override
    public Set<String> getSupportedProtocols()
    {
        HashSet<String> hsProtocols = new HashSet<String>();

        hsProtocols.add("http");
        hsProtocols.add("https");

        return hsProtocols;
    }




}
