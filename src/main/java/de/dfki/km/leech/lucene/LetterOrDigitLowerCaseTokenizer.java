package de.dfki.km.leech.lucene;



import java.io.Reader;

import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.Version;



/**
 * Tokenizer that tokenizes between letter and digit entries. The chars will also be converted to lower case.
 * 
 * Note: this does a decent job for most European languages, but does a terrible job for some Asian languages, where words maybe are not separated by
 * spaces, etc.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class LetterOrDigitLowerCaseTokenizer extends CharTokenizer
{

    /**
     * Creates a new {@link LetterOrDigitLowerCaseTokenizer} Object
     * 
     * @param matchVersion the lucene version to use 
     * @param input the input reader for the Tokenizer
     */
    public LetterOrDigitLowerCaseTokenizer(Version matchVersion, Reader input)
    {
        super(matchVersion, input);
    }




    /**
     * Collects only characters which satisfy {@link Character#Character.isLetterOrDigit(int)}.
     */
    @Override
    protected boolean isTokenChar(int c)
    {
        return Character.isLetterOrDigit(c);
    }



    /**
     * Converts char to lower case {@link Character#toLowerCase(int)}.
     */
    @Override
    protected int normalize(int c)
    {
        return Character.toLowerCase(c);
    }
}
