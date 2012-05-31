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



import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;

import de.dfki.km.leech.parser.filter.URLFilter;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;



/**
 * A class to give a context / configuration for the crawling process. In the CrawlerContext class you find all configuration issues that are common
 * for all crawling parser implementations. There exists also context implementations with configurations that are special for a specific
 * CrawlerParser, e.g. {@link DirectoryCrawlerContext}. For them, have a look to the other classes of this package. Aspects as e.g. incremental
 * indexing, or even stopping a running crawling process can be set here. An Object of this class can be given to the ParseContext.<br>
 * Examples:<br>
 * <code>
 * CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");<br><br>
 * URLFilter boundaries = new URLFilter().addIncludePattern(new SubstringPattern("www.leech.de", SubstringPattern.STARTS_WITH));<br>
 * crawlerContext.setURLFilter(boundaries);<br>
 * <br>
 * ParseContext parseContext = new ParseContext();<br>
 * parseContext.set(CrawlerContext.class, crawlerContext);<br>
 * </code> or for convinience<br>
 * <code>
 * ParseContext parseContext = crawlerContext.createParseContext()<br>
 * </code>
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class CrawlerContext
{

    protected Boolean m_bCheckForRemovedEntities = true;

    protected Boolean m_bDetectCycles = true;

    protected Boolean m_bInterruptIfException = false;

    protected Boolean m_bStopRequested = false;

    protected Boolean m_bVerbose = false;

    protected ContentHandler m_contentHandler;

    protected int m_crawlingDepth = Integer.MAX_VALUE;

    protected IncrementalCrawlingHistory m_incrementalCrawlingHistory;

    protected String m_strContentHandlerClassName;;

    protected String m_strIncrementalCrawlingHistoryPath;

    protected URLFilter m_urlFilter = new URLFilter();

    /**
     * Creates a new ParseContext Object with an entry with this {@link #CrawlerContext} configuration. This method is only for convenience.
     * 
     * @return the created ParseContext Object.
     */
    public ParseContext createParseContext()
    {
        ParseContext parseContext = new ParseContext();
        parseContext.set(CrawlerContext.class, this);

        return parseContext;
    }

    /**
     * Gets whether or not the crawler should check for removed entities after the crawl. All entities that are not 'touched' during a crawl but has
     * an entry inside the history will be considered as removed. This works good in the case you watch e.g. a directory periodically, and all new
     * differences should be reflected. Nevertheless, there could be the situation where you have an existing history (e.g. for a directory) and want
     * to add a single entity (e.g. file) with a Leech call, to add and process it only in the case it was modified or new with respect to its
     * potential history entry. In this case you don't want to flag all other existing entries beside this one file as removed. In this situation, you
     * can disable the 'removed entity check' with this method.
     * 
     * @return true: all files that were processed during a former crawl and has an history entry, but didn't processed/touched during this crawl will
     *         be arked as removed. False otherwise.
     */
    public Boolean getCheckForRemovedEntities()
    {
        return m_bCheckForRemovedEntities;
    }

    /**
     * Gets the contentHandler that will be used during the crawl - and thus for every recursive call.
     * 
     * @return the contentHandler that will be used during the crawl - and thus for every recursive call.
     */
    public ContentHandler getContentHandler()
    {
        return m_contentHandler;
    }

    /**
     * Gets the class name for the content handler that should be instantiated on every recursive call during the crawl. In the case it is null or no
     * CrawlerConfig is used at all, Leech will reuse the given contenthandler, which is only possible if this instance is in a reusable state after a
     * parse operation (e.g. non re-initialised writers, etc.). Make sure that this is the case by e.g. clearing the internal states inside the
     * endDocument() method.
     * 
     * @return the class name for the content handler that should be instantiated on every recursive call during the crawl
     */
    public String getContentHandlerClassName()
    {
        return m_strContentHandlerClassName;
    }



    /**
     * Gets the maximum depth of recursive calls the crawling process will follow. Default is Integer.MAX_VALUE
     * 
     * @return the maximum depth of recursive calls the crawling process will follow. Default is Integer.MAX_VALUE
     */
    public int getCrawlingDepth()
    {
        return m_crawlingDepth;
    }



    /**
     * Gets whether the crawlers should detect cycles during the crawl or not. Cycle detection might be not necessary e.g. when you crawl a file
     * system directory without following symbolic links. Nevertheless you could run into a hard link cycle. Cycle detection is important when you
     * e.g. crawl websites, where links easily can result into cyclic structures. If cycle detection is enabled, Leech simply enables a temporar
     * incremental crawling history for this crawl, that will be removed after the crawl. This also means that when you index incrementally by
     * specifying an incremental crawling history, cycle detection is given anyway - no further history will be created by enabling cycle detection
     * with this CrawlerContext object. The default is the enabled cycle detection - which is more or less a no-brainer, unless you have really hard
     * performance constraints.
     * 
     * @return true in the case cycle detection is enabled, false otherwise. Note that if you specify an incrementyl crawling history, cycle detection
     *         is given anyway.
     */
    public Boolean getDetectCycles()
    {
        return m_bDetectCycles;
    }



    /**
     * Gets an IncrementalCrawlingHistory Object for the configured IncrementalCrawlingHistoryPath. At first invocation, the history Object will be
     * created.
     * 
     * @return the IncrementalCrawlingHistory Object for the configured IncrementalCrawlingHistoryPath, null in the case no path is configured.
     */
    public IncrementalCrawlingHistory getIncrementalCrawlingHistory()
    {
        if(m_strIncrementalCrawlingHistoryPath == null) return null;

        if(m_incrementalCrawlingHistory == null) m_incrementalCrawlingHistory = new IncrementalCrawlingHistory(m_strIncrementalCrawlingHistoryPath);


        return m_incrementalCrawlingHistory;
    }



    /**
     * Gets a path to the incremental crawling history. In the case a path is specified, the crawlers will use incremental parsing, which means that
     * they check whether a data entity is new, modified or deleted. Time consuming extraction will only performed in the new- and modified case. A
     * DataSinkContentHandler will take care that deleted entities will also deleted from the data sink. In the case an entity has not changed at all,
     * no extraction of the data will be performed. In the case the path is specified as null or empty, no crawling history will be used - everthing
     * will be simply extracted.
     * 
     * @return the path to the crawling history that is used - null or empty in the case no crawling history will be used (which is the default)
     */
    public String getIncrementalCrawlingHistoryPath()
    {
        return m_strIncrementalCrawlingHistoryPath;
    }



    /**
     * Gets whether the whole crawling process will be interrupted in the case of an Exception while processing one data entityor not
     * 
     * @return true: the whole crawling process will be interrupted in the case of an exception, false otherwise. The default is to not interrupt.
     */
    public Boolean getInterruptIfException()
    {
        return m_bInterruptIfException;
    }



    /**
     * Gets the domain boundaries to constrain the data entities that should be considered during this crawl
     * 
     * @return the domain boundaries to constrain the data entities that should be considered during this crawl. The default is a domainboundary that
     *         skips nothing.
     */
    public URLFilter getURLFilter()
    {
        return m_urlFilter;
    }



    /**
     * Gets whether the crawling process is verbose or not
     * 
     * @return true: verbosity on, false otherwise
     */
    public Boolean getVerbose()
    {
        return m_bVerbose;
    }



    /**
     * Request to stop the crawling process. The method will wait until the crawling process is stopped (by performing a wait() on the return value of
     * stopRequested()). The currently running crawler will call a notify when finished.
     */
    public void requestStop()
    {
        if(m_bStopRequested == true) return;

        m_bStopRequested = true;

        synchronized (m_bStopRequested)
        {
            try
            {
                m_bStopRequested.wait();

                m_bStopRequested = false;
            }
            catch (InterruptedException e)
            {
                Logger.getLogger(CrawlerContext.class.getName()).log(Level.SEVERE, "Error", e);
            }
        }
    }




    /**
     * Sets whether or not the crawler should check for removed entities after the crawl. All entities that are not 'touched' during a crawl but has
     * an entry inside the history will be considered as removed. This works good in the case you watch e.g. a directory periodically, and all new
     * differences should be reflected. Nevertheless, there could be the situation where you have an existing history (e.g. for a directory) and want
     * to add a single entity (e.g. file) with a Leech call, to add and process it only in the case it was modified or new with respect to its
     * potential history entry. In this case you don't want to flag all other existing entries beside this one file as removed. In this situation, you
     * can disable the 'removed entity check' with this method.
     * 
     * @param checkForRemovedEntities true: all files that were processed during a former crawl and has an history entry, but didn't processed/touched
     *            during this crawl will be arked as removed. False otherwise.
     * 
     * @return this
     */
    public CrawlerContext setCheckForRemovedEntities(Boolean checkForRemovedEntities)
    {
        m_bCheckForRemovedEntities = checkForRemovedEntities;

        return this;
    }



    /**
     * Sets the contentHandler that will be used during the crawl - and thus for every recursive call. You can specify the contentHandler either here,
     * inside the CrawlerContext, or simply use one of the Leech methods with an contentHandler parameter. These methods simply invoke this
     * setContentHandler method. In the case the ContentHandlerClassName was set, this contentHandler object will be ignored.
     * 
     * @param contentHandler the ContentHandler that should be used during the crawl
     * 
     * @return this for convenience
     */
    public CrawlerContext setContentHandler(ContentHandler contentHandler)
    {
        m_contentHandler = contentHandler;

        return this;
    }



    /**
     * Specifies the class name for the content handler that should be instantiated on every recursive call during the crawl. In the case it is null
     * or no CrawlerConfig is used at all, Leech will reuse the given contenthandler object also specified inside setContentHandler, which is only
     * possible if this object is in a reusable state after a parse operation (e.g. non re-initialised writers, etc.). Make sure that this is the case
     * by e.g. clearing the internal states inside the endDocument() method.
     * 
     * @param strContentHandlerClassName the class name for the content handler that should be instantiated on every recursive call during the crawl
     * 
     * @return this with the new entry. For convenience.
     */
    public CrawlerContext setContentHandlerClassName(String strContentHandlerClassName)
    {
        m_strContentHandlerClassName = strContentHandlerClassName;

        return this;
    }



    /**
     * Sets the maximum depth of recursive calls the crawling process will follow. Default is Integer.MAX_VALUE
     * 
     * @param crawlingDepth the maximum depth of recursive calls the crawling process will follow. Default is Integer.MAX_VALUE
     * 
     * @return this with the new entry. For convenience.
     */
    public CrawlerContext setCrawlingDepth(int crawlingDepth)
    {
        m_crawlingDepth = crawlingDepth;

        return this;
    }



    /**
     * Sets whether the crawlers should detect cycles during the crawl or not. Cycle detection might be not necessary e.g. when you crawl a file
     * system directory without following symbolic links. Nevertheless you could run into a hard link cycle. Cycle detection is important when you
     * e.g. crawl websites, where links easily can result into cyclic structures. If cycle detection is enabled, Leech simply enables a temporar
     * incremental crawling history for this crawl, that will be removed after the crawl. This also means that when you index incrementally by
     * specifying an incremental crawling history, cycle detection is given anyway - no further history will be created by enabling cycle detection
     * with this method. The default is the enabled cycle detection.
     * 
     * @param detectCycles true in the case you want to enable cycle detection (default), false otherwise. In the case you specified incremental
     *            indexing, cycle detection is given anyway. (but you don't have to disable it with this method for performance reasons).
     */
    public void setDetectCycles(Boolean detectCycles)
    {
        m_bDetectCycles = detectCycles;
    }



    /**
     * Sets a path to the incremental crawling history. In the case a path is specified, the crawlers will use incremental parsing, which means that
     * they check whether a data entity is new, modified or deleted. Time consuming extraction will only performed in the new- and modified case. A
     * DataSinkContentHandler will take care that deleted entities will also deleted from the data sink. In the case an entity has not changed at all,
     * no extraction of the data will be performed. In the case the path is specified as null or empty, no crawling history will be used - everthing
     * will be simply extracted.
     * 
     * @param strIncrementalCrawlingHistoryPath the path to the crawling history that should be used - null or empty in the case no crawling history
     *            should be used (which is the default)
     * 
     * @return this with the new entry. For convenience.
     */
    public CrawlerContext setIncrementalCrawlingHistoryPath(String strIncrementalCrawlingHistoryPath)
    {
        m_strIncrementalCrawlingHistoryPath = strIncrementalCrawlingHistoryPath;

        return this;
    }



    /**
     * In the case there is an Exception when one data entity is processed, the whole crawling process will be interrupted or not. The default is to
     * not interrupt.
     * 
     * @param bInterruptIfException true: the whole crawling process will be interrupted in the case of an exception, false otherwise.
     * 
     * @return this with the new entry. For convenience.
     */
    public CrawlerContext setInterruptIfException(Boolean bInterruptIfException)
    {
        m_bInterruptIfException = bInterruptIfException;

        return this;
    }



    /**
     * URLFilter uses patterns (regular expressions or substrings checks) to determine whether a URL/source string belongs to a datasource domain or
     * not. Use these pattern in order you want to constrain the crawling process to some root directories, web domains, ... or if you want to exclude
     * some specific directories/files/links, etc. <br>
     * Example:<br>
     * <code>
     * URLFilter boundaries = new URLFilter().addIncludePattern(new SubstringPattern("www.leech.de", SubstringPattern.STARTS_WITH));
     * </code> <br>
     * or <br>
     * <code>
     * URLFilter boundaries = new URLFilter().addExcludePattern(new SubstringPattern("liquorice", SubstringPattern.CONTAINS));
     * </code>
     * 
     * 
     * @param urlFilter the domain boundaries to constrain the data entities that should be considered during this crawl
     * 
     * @return this with the new entry. For convenience.
     */
    public CrawlerContext setURLFilter(URLFilter urlFilter)
    {
        m_urlFilter = urlFilter;

        return this;
    }



    /**
     * Sets the crawling process to verbose. Some messages as skipped entities will be shown additionally
     * 
     * @param bVerbose true: verbosity on, false otherwise
     * 
     * @return this with the new entry. For convenience.
     */
    public CrawlerContext setVerbose(Boolean bVerbose)
    {
        m_bVerbose = bVerbose;
        
        return this;
    }



    /**
     * Used to check by a crawler or other parser implementation whether a stop was requested or not. In the case a stop was requested, the parser
     * implementation has to call a notify() or notifyAll() to the returned Boolean Object when finished, to 'wake up' the waiting
     * {@link #requestStop()} method.
     * 
     * @return true in the case a stop was requested. Don't forget to call a notify() on the returned Object in order to 'wake up' the waiting
     *         {@link #requestStop()} method.
     */
    public Boolean stopRequested()
    {
        return m_bStopRequested;
    }

}
