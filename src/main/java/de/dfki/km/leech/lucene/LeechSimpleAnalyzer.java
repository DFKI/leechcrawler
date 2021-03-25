package de.dfki.km.leech.lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
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
    protected TokenStreamComponents createComponents(String fieldName)
    {
        return new TokenStreamComponents(new LetterOrDigitLowerCaseTokenizer());
    }
}
