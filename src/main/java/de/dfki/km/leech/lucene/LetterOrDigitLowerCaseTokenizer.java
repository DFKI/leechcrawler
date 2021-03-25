package de.dfki.km.leech.lucene;



import org.apache.lucene.analysis.util.CharTokenizer;
import org.apache.lucene.util.AttributeFactory;



/**
 * Tokenizer that tokenizes between letter and digit entries. The chars will also be converted to lower case.
 * <p>
 * Note: this does a decent job for most European languages, but does a terrible job for some Asian languages, where words maybe are not separated by
 * spaces, etc.
 *
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class LetterOrDigitLowerCaseTokenizer extends CharTokenizer
{

    public LetterOrDigitLowerCaseTokenizer(AttributeFactory factory)
    {
        super(factory);
    }



    public LetterOrDigitLowerCaseTokenizer()
    {
        super();
    }




    /**
     * Collects only characters which satisfy {@link Character#isLetterOrDigit(int)}.
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
