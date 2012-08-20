package de.dfki.km.leech.lucene;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.util.Version;



/**
 * An {@link Analyzer} that filters {@link LetterOrDigitLowerCaseTokenizer} with {@link LowerCaseFilter}
 **/
public class LeechSimpleAnalyzer extends ReusableAnalyzerBase
{



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
