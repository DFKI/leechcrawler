package de.dfki.km.leech.parser.filter;



/**
 * A {@link URLFilterPattern} defines a boolean pattern test on a URL/source string.
 */
public interface URLFilterPattern
{

    /**
     * Apply the pattern matching test on a URL/source string.
     * 
     * @param strUrlOrSource The URL/source string to test the pattern on.
     * @return 'true' when the URL/source string matches this {@link URLFilterPattern}, 'false' otherwise.
     */
    public boolean matches(String strUrlOrSource);

}
