/*
    Leech - crawling capabilities for Apache Tika
    
    Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Contact us by mail: christian.reuschling@dfki.de
*/

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
        this(Pattern.compile(pattern, Pattern.UNICODE_CHARACTER_CLASS));
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
        this.pattern = Pattern.compile(pattern, Pattern.UNICODE_CHARACTER_CLASS);
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
