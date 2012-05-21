package de.dfki.km.leech.config;



import de.dfki.km.leech.parser.DirectoryCrawlerParser;



/**
 * This class gives a context / configuration for the crawling process, with additional configuration capabilities that are special to the
 * {@link DirectoryCrawlerParser}. Aspects as e.g. if the crawler should follow symbolic links or consider hidden files can be adjusted.<br>
 * <br>
 * Examples:<br>
 * <code>
 * CrawlerContext crawlerContext = new CrawlerContext().setIncrementalCrawlingHistoryPath("./history/forResourceDir");<br><br>
 * 
 * DirectoryCrawlerContext directoryCrawlerContext = new DirectoryCrawlerContext().setIgnoreHiddenFiles(false);<br><br>
 * ParseContext parseContext = new ParseContext();<br>
 * parseContext.set(CrawlerContext.class, crawlerContext);<br>
 * parseContext.set(DirectoryCrawlerContext.class, directoryCrawlerContext);<br>
 * 
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class DirectoryCrawlerContext
{

    protected boolean m_followSymbolicLinks = false;

    protected boolean m_ignoreHiddenFiles = true;




    /**
     * Gets whether the crawler should follow symbolic links or not. The default is false.
     * 
     * @return true: the crawler will follow symbolic links, false otherwise
     */
    public boolean getFollowSymbolicLinks()
    {
        return m_followSymbolicLinks;
    }



    /**
     * Gets whether the crawler ignores hidden files or not. The default is true.
     * 
     * @return true: the crawler will ignore hidden files, false otherwise
     */
    public boolean getIgnoreHiddenFiles()
    {
        return m_ignoreHiddenFiles;
    }



    /**
     * Specifies whether the crawler should follow symbolic links or not. The default is false.
     * 
     * @param followSymbolicLinks true: the crawler will follow symbolic links, false otherwise
     * 
     * @return this for convenience
     */
    public DirectoryCrawlerContext setFollowSymbolicLinks(boolean followSymbolicLinks)
    {
        m_followSymbolicLinks = followSymbolicLinks;

        return this;
    }



    /**
     * Specifies whether the crawler ignores hidden files or not. The default is true.
     * 
     * @param ignoreHiddenFiles true: the crawler will ignore hidden files, false otherwise
     * 
     * @return this for convenience
     */
    public DirectoryCrawlerContext setIgnoreHiddenFiles(boolean ignoreHiddenFiles)
    {
        m_ignoreHiddenFiles = ignoreHiddenFiles;

        return this;
    }

}
