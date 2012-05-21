package de.dfki.km.leech.parser.filter;



import java.util.regex.Pattern;



/**
 * A {@link URLFilterPattern} implementation using a regular expression evaluation strategy.
 */
public class RegExpPattern implements URLFilterPattern
{

    protected Pattern pattern;



    public RegExpPattern(Pattern pattern)
    {
        this.pattern = pattern;
    }

    
    public RegExpPattern(String pattern)
    {
        this(Pattern.compile(pattern));
    }



    @Override
    public boolean equals(Object obj)
    {
        boolean result = this == obj;

        if(!result && obj instanceof RegExpPattern)
        {
            RegExpPattern other = (RegExpPattern) obj;
            result = getPatternString().equals(other.getPatternString());
        }

        return result;
    }


    public Pattern getPattern()
    {
        return pattern;
    }



    public String getPatternString()
    {
        return pattern.pattern();
    }



    @Override
    public int hashCode()
    {
        return getPatternString().hashCode();
    }



    @Override
    public boolean matches(String strUrlOrSource)
    {
        return pattern.matcher(strUrlOrSource).matches();
    }



    public void setPattern(Pattern pattern)
    {
        this.pattern = pattern;
    }



    public void setPattern(String pattern)
    {
        this.pattern = Pattern.compile(pattern);
    }




    /**
     * Creates a new {@link URLFilter} Object and adds this pattern as exclude pattern
     * 
     * @return the resulting {@link URLFilter} Object
     */
    public URLFilter toURLFilterAsExclude()
    {
        return new URLFilter().addIncludePattern(this);
    }



    /**
     * Creates a new {@link URLFilter} Object and adds this pattern as include pattern
     * 
     * @return the resulting {@link URLFilter} Object
     */
    public URLFilter toURLFilterAsInclude()
    {
        return new URLFilter().addIncludePattern(this);
    }

}
