package de.dfki.km.leech.lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.util.Version;



/**
 * An {@link Analyzer} that filters {@link LetterOrDigitLowerCaseTokenizer} with {@link LowerCaseFilter}
 **/
public class LeechSimpleAnalyzer extends Analyzer
{

    static final protected LeechSimpleAnalyzer m_singelton = new LeechSimpleAnalyzer();
    
    static public LeechSimpleAnalyzer getSingleton()
    {
        return m_singelton;
    }


    /**
     * Creates a new {@link LeechSimpleAnalyzer}
     */
    public LeechSimpleAnalyzer()
    {
    }




    @Override
    protected TokenStreamComponents createComponents(final String fieldName, final Reader reader)
    {
        return new TokenStreamComponents(new LetterOrDigitLowerCaseTokenizer(Version.LUCENE_CURRENT, reader));
    }
}
