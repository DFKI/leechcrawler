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

package de.dfki.km.leech.util;



import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.mail.URLName;




/**
 * Offers utility methods related to URLs, network connections, etc.
 */
public class UrlUtil
{

    /**
     * Remove relative references and "mistakes" like double slashes from the path.
     * 
     * @param path The path to normalize.
     * @return The normalized path.
     */
    public static String normalizePath(String path)
    {
        String result = path;

        // replace all double slashes with a single slash
        result = replace("//", "/", result);

        // replace all references to the current directory with nothing
        result = replace("/./", "/", result);

        // replace all references to the parent directory with nothing
        result = result.replaceAll("/[^/]+/\\.\\./", "/");

        return result;
    }



    public static String extractFolder(URLName url)
    {
        String strFolder = url.getFile();
        int iIndex = strFolder.indexOf(";");
        if(iIndex > 0) strFolder = strFolder.substring(0, iIndex);
        while (strFolder.endsWith("/"))
            strFolder = strFolder.substring(0, strFolder.length() - 1);

        return strFolder;
    }



    public static String extractUID(URLName url)
    {
        String strUID = url.toString();

        int iIndex = strUID.toLowerCase().indexOf("uid=");
        if(iIndex == -1) return null;

        strUID = strUID.substring(iIndex + 4);
        iIndex = strUID.indexOf(";");
        if(iIndex > 0) strUID = strUID.substring(0, iIndex);

        return strUID;
    }



    /**
     * Substitute String "old" by String "new" in String "text" everywhere.
     * 
     * @param olds The String to be substituted.
     * @param news The String containing the new content.
     * @param text The String in which the substitution is done.
     * @return The result String containing the substitutions; if no substitutions were made, the specified 'text' instance is returned.
     */
    protected static String replace(String olds, String news, String text)
    {
        if(olds == null || olds.length() == 0)
        {
            // nothing to substitute.
            return text;
        }
        if(text == null)
        {
            return null;
        }

        // search for any occurences of 'olds'.
        int oldsIndex = text.indexOf(olds);
        if(oldsIndex == -1)
        {
            // Nothing to substitute.
            return text;
        }

        // we're going to do some substitutions.
        StringBuilder buffer = new StringBuilder(text.length());
        int prevIndex = 0;

        while (oldsIndex >= 0)
        {
            // first, add the text between the previous and the current occurence
            buffer.append(text.substring(prevIndex, oldsIndex));

            // then add the substition pattern
            buffer.append(news);

            // remember the index for the next loop
            prevIndex = oldsIndex + olds.length();

            // search for the next occurence
            oldsIndex = text.indexOf(olds, prevIndex);
        }

        // add the part after the last occurence
        buffer.append(text.substring(prevIndex));

        return buffer.toString();
    }



    /**
     * Normalizes a query string by sorting the query parameters alpabetically.
     * 
     * @param query The query string to normalize.
     * @return The normalized query string.
     */
    public static String normalizeQuery(String query)
    {
        TreeSet<String> sortedSet = new TreeSet<String>();

        // extract key-value pairs from the query string
        StringTokenizer tokenizer = new StringTokenizer(query, "&");
        while (tokenizer.hasMoreTokens())
        {
            sortedSet.add(tokenizer.nextToken());
        }

        // reconstruct query string
        StringBuilder result = new StringBuilder(query.length());

        Iterator<String> iterator = sortedSet.iterator();
        while (iterator.hasNext())
        {
            result.append(iterator.next());

            if(iterator.hasNext())
            {
                result.append('&');
            }
        }

        return result.toString();
    }



    /**
     * Normalizes a URL. The following steps are taken to normalize a URL:
     * 
     * <ul>
     * <li>The protocol is made lower-case.
     * <li>The host is made lower-case.
     * <li>A specified port is removed if it matches the default port.
     * <li>Any query parameters are sorted alphabetically.
     * <li>Any anchor information is removed.
     * </ul>
     * 
     * @param url the url that should be normalized
     * 
     * @return the normalized url
     */
    public static URLName normalizeURL(URLName url)
    {
        try
        {
            // retrieve the various parts of the URL
            String protocol = url.getProtocol();
            String host = url.getHost();
            int port = url.getPort();
            String path = url.getFile();
            String query = url.getRef();
            String username = url.getUsername();
            String password = url.getPassword();

            // normalize the fields
            protocol = protocol.toLowerCase();
            host = host.toLowerCase();


            String file = "";
            if(path != null) file = normalizePath(path);

            if(query != null)
            {
                query = normalizeQuery(query);
                file += "?" + query;
            }

            // create the normalized URL
            url = new URLName(protocol, host, port, file, username, password);

            return url;

        }
        catch (Exception e)
        {
            throw new RuntimeException("Error while normalizing the url " + url.toString() + ". Is it a well formed URL? ");
        }
    }
}
