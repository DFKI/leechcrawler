package de.dfki.km.leech.lucene;



import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.LowerCaseFilter;



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
