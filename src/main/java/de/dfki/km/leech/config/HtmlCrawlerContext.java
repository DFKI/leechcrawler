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



import de.dfki.km.leech.parser.HtmlCrawlerParser;



/**
 * This class gives a context / configuration for the crawling process, with additional configuration capabilities that are special to the
 * {@link HtmlCrawlerParser}. Aspects as e.g. if the crawler should follow remote links if she currently crawls a local html file can be adjusted.<br>
 * <br>
 * Examples:<br>
 * <code>
 * CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");<br><br>
 * 
 * HtmlCrawlerContext htmlCrawlerContext = new HtmlCrawlerContext().setFollowRemoteLinksIfLocalFileCrawl(true);<br><br>
 * ParseContext parseContext = new ParseContext();<br>
 * parseContext.set(CrawlerContext.class, crawlerContext);<br>
 * parseContext.set(HtmlCrawlerContext.class, htmlCrawlerContext);<br>
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class HtmlCrawlerContext
{


    protected boolean m_followRemoteLinksIfLocalFileCrawl = false;



    /**
     * Gets whether or not remote links should be followed if the html crawlerParser crawls a file on the local file system. Remote links are links
     * with an URL protocoll different from 'file:'. The default is false.
     * 
     * @return true in the case remote links will be followed, false otherwise
     */
    public boolean getFollowRemoteLinksIfLocalFileCrawl()
    {
        return m_followRemoteLinksIfLocalFileCrawl;
    }



    /**
     * Sets whether or not remote links should be followed if the html crawlerParser crawls a file on the local file system. Remote links are links
     * with an URL protocoll different from 'file:'. The default is false.
     * 
     * @param followRemoteLinksIfLocalFileCrawl true in the case remote links will be followed, false otherwise.
     * 
     * @return this for convinience
     */
    public HtmlCrawlerContext setFollowRemoteLinksIfLocalFileCrawl(boolean followRemoteLinksIfLocalFileCrawl)
    {
        m_followRemoteLinksIfLocalFileCrawl = followRemoteLinksIfLocalFileCrawl;

        return this;
    }





}
