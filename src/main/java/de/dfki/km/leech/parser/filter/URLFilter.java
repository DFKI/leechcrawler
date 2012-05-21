package de.dfki.km.leech.parser.filter;



import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;



/**
 * URLFilter uses patterns (regular expressions or substrings checks) to determine whether a URL/source string belongs to a datasource domain or not.
 * 
 * <p>
 * Each URLFilter maintains lists of include and exclude patterns. A URL/source string is matched against these two pattern lists to determine whether
 * it is inside or outside the domain. A URL/source string is inside the domain when it matches at least one of the include patterns but none of the
 * exclude patterns. In case no include patterns are specified, all URLs that don't match any of the exclude patterns are included.<br>
 * <br>
 * Examples:<br>
 * <br>
 * URLFilter URLFilter = new URLFilter().addIncludePattern(new SubstringPattern("file:", SubstringPattern.STARTS_WITH));<br>
 * <br>
 * ParseContext parseContext = new ParseContext();<br>
 * parseContext.set(CrawlerContext.class, new CrawlerContext.setURLFilter(URLFilter));<br>
 */
public class URLFilter implements FilenameFilter
{

    protected LinkedList<URLFilterPattern> m_llExcludePatterns;

    protected LinkedList<URLFilterPattern> m_llIncludePatterns;



    public URLFilter()
    {
        this(new LinkedList<URLFilterPattern>(), new LinkedList<URLFilterPattern>());
    }



    public URLFilter(List<URLFilterPattern> lIncludePatterns, List<URLFilterPattern> lExcludePatterns)
    {
        this.m_llIncludePatterns = new LinkedList<URLFilterPattern>(lIncludePatterns);
        this.m_llExcludePatterns = new LinkedList<URLFilterPattern>(lExcludePatterns);
    }




    @Override
    public boolean accept(File dir, String name)
    {
        return accept(name);
    }



    /**
     * Checks whether the supplied URL/source string falls inside the specified boundaries.
     * 
     * @param strUrlOrSource The URL/source string to check.
     * @return 'true' if the URL/source string is inside the crawl domain, 'false' otherwise.
     */
    public boolean accept(String strUrlOrSource)
    {
        URLFilterPattern pattern;

        boolean insideDomain = false;

        int nrIncludePatterns = m_llIncludePatterns.size();
        if(nrIncludePatterns == 0)
        {
            insideDomain = true;
        }
        else
        {
            for (int i = 0; i < nrIncludePatterns; i++)
            {
                pattern = m_llIncludePatterns.get(i);
                if(pattern.matches(strUrlOrSource))
                {
                    insideDomain = true;
                    break;
                }
            }
        }

        if(insideDomain)
        {
            int nrExcludePatterns = m_llExcludePatterns.size();
            for (int i = 0; i < nrExcludePatterns; i++)
            {
                pattern = m_llExcludePatterns.get(i);
                if(pattern.matches(strUrlOrSource))
                {
                    insideDomain = false;
                    break;
                }
            }
        }

        return insideDomain;
    }



    /**
     * Checks whether the supplied URL falls inside the specified boundaries.
     * 
     * @param url The URL
     * @return 'true' if the URL/source string is inside the crawl domain, 'false' otherwise.
     */
    public boolean accept(URL url)
    {
        return accept(url.toString());
    }



    /**
     * Adds an exclude pattern
     * 
     * @param pattern the new exclude pattern
     * 
     * @return this with the new entry. For convenience.
     */
    public URLFilter addExcludePattern(URLFilterPattern... pattern)
    {
        m_llExcludePatterns.addAll(Arrays.asList(pattern));

        return this;
    }



    /**
     * Adds an include pattern
     * 
     * @param pattern the new include pattern
     * 
     * @return this with the new entry. For convenience.
     */
    public URLFilter addIncludePattern(URLFilterPattern... pattern)
    {
        m_llIncludePatterns.addAll(Arrays.asList(pattern));

        return this;
    }



    /**
     * @return a read-only version of the internal exclude-list
     */
    public List<URLFilterPattern> getExcludePatterns()
    {
        return Collections.unmodifiableList(m_llExcludePatterns);
    }



    /**
     * @return a read-only version of the internal include-list
     */
    public List<URLFilterPattern> getIncludePatterns()
    {
        return Collections.unmodifiableList(m_llIncludePatterns);
    }



    /**
     * Removes all exclude pattern
     * 
     * @return this for convenience
     */
    public URLFilter removeAllExcludePatterns()
    {
        m_llExcludePatterns.clear();

        return this;
    }



    /**
     * Removes all include pattern
     * 
     * @return this for convenience
     */
    public URLFilter removeAllIncludePatterns()
    {
        m_llIncludePatterns.clear();

        return this;
    }



    /**
     * Removes all pattern
     * 
     * @return this for convenience
     */
    public URLFilter removeAllPatterns()
    {
        removeAllIncludePatterns();
        removeAllExcludePatterns();

        return this;
    }



    public boolean removeExcludePattern(URLFilterPattern... pattern)
    {
        return m_llExcludePatterns.removeAll(Arrays.asList(pattern));
    }



    public boolean removeIncludePattern(URLFilterPattern... pattern)
    {
        return m_llIncludePatterns.removeAll(Arrays.asList(pattern));
    }



    /**
     * Sets a new exclude pattern list
     * 
     * @param lExcludePatterns the new exclude patterns
     * 
     * @return this with the new entry. For convenience.
     */
    public URLFilter setExcludePatterns(List<URLFilterPattern> lExcludePatterns)
    {
        this.m_llExcludePatterns = new LinkedList<URLFilterPattern>(lExcludePatterns);

        return this;
    }



    /**
     * Sets a new exclude pattern list
     * 
     * @param lIncludePatterns the new exclude patterns
     * 
     * @return this with the new entry. For convenience.
     */
    public URLFilter setIncludePatterns(List<URLFilterPattern> lIncludePatterns)
    {
        this.m_llIncludePatterns = new LinkedList<URLFilterPattern>(lIncludePatterns);

        return this;
    }
}
