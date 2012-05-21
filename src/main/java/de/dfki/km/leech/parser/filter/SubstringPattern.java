package de.dfki.km.leech.parser.filter;






/**
 * A {@link URLFilterPattern} implementation using a substring test evaluation strategy.
 */
public class SubstringPattern implements URLFilterPattern
{

    protected static class Contains extends SubstringCondition
    {

        @Override
        public boolean test(String string, String substring)
        {
            return string.indexOf(substring) >= 0;
        }



        @Override
        public String toString()
        {
            return "Contains";
        }

    }




    protected static class DoesNotContain extends SubstringCondition
    {

        @Override
        public boolean test(String string, String substring)
        {
            return string.indexOf(substring) < 0;
        }



        @Override
        public String toString()
        {
            return "DoesNotContain";
        }

    }




    protected static class EndsWith extends SubstringCondition
    {

        @Override
        public boolean test(String string, String substring)
        {
            return string.endsWith(substring);
        }



        @Override
        public String toString()
        {
            return "EndsWith";
        }

    }




    protected static class StartsWith extends SubstringCondition
    {

        @Override
        public boolean test(String string, String substring)
        {
            return string.startsWith(substring);
        }



        @Override
        public String toString()
        {
            return "StartsWith";
        }

    }




    /**
     * Instances of this class indicate how a substring test needs to be performed and are able to evaluate the test. Subclasses embody a particular
     * kind of substring test, e.g. "starts with", "ends with" or "contains".
     */
    protected static abstract class SubstringCondition
    {

        @Override
        public boolean equals(Object obj)
        {
            boolean result = this == obj;

            if(!result && obj instanceof SubstringCondition)
            {
                SubstringCondition other = (SubstringCondition) obj;
                result = toString().equals(other.toString());
            }

            return result;
        }



        @Override
        public int hashCode()
        {
            return toString().hashCode();
        }



        /**
         * Tests the substring condition embodied by the implementing class on a String.
         * 
         * @param strString2Test The String to test the substring condition on.
         * @param strSubstring The String to test the substring condition with.
         * @return 'true' when the string contains the substring in the way embodied by the implementing class, 'false' otherwise.
         */
        public abstract boolean test(String strString2Test, String strSubstring);

    }




    public static final Contains CONTAINS = new Contains();




    public static final DoesNotContain DOES_NOT_CONTAIN = new DoesNotContain();


    public static final EndsWith ENDS_WITH = new EndsWith();


    public static final StartsWith STARTS_WITH = new StartsWith();


    protected SubstringCondition m_condition;


    protected String m_strSubstring;






    /**
     * Creates a new SubStringPattern. For the condition, use the constants defined in this class
     * 
     * @param strSubstring the Url/source string you want to check against the SubstringPattern
     * @param condition the condition, how the substing should be checked. One out of {@link #CONTAINS}, {@link #DOES_NOT_CONTAIN}, {@link #ENDS_WITH}
     *            or {@link #STARTS_WITH}
     */
    public SubstringPattern(String strSubstring, SubstringCondition condition)
    {
        this.m_strSubstring = strSubstring;
        this.m_condition = condition;
    }



    @Override
    public boolean equals(Object obj)
    {
        boolean result = this == obj;

        if(!result && obj instanceof SubstringPattern)
        {
            SubstringPattern other = (SubstringPattern) obj;
            result = getSubstring().equals(other.getSubstring()) && getCondition().equals(other.getCondition());
        }

        return result;
    }



    public SubstringCondition getCondition()
    {
        return m_condition;
    }



    public String getSubstring()
    {
        return m_strSubstring;
    }



    @Override
    public int hashCode()
    {
        return toString().hashCode();
    }



    @Override
    public boolean matches(String strUrlOrSource)
    {
        return m_condition.test(strUrlOrSource, m_strSubstring);
    }



    /**
     * Sets the condition
     * 
     * @param condition the condition, how the substing should be checked. One out of {@link #CONTAINS}, {@link #DOES_NOT_CONTAIN}, {@link #ENDS_WITH}
     *            or {@link #STARTS_WITH}
     */
    public void setCondition(SubstringCondition condition)
    {
        this.m_condition = condition;
    }



    public void setSubstring(String strSubstring)
    {
        this.m_strSubstring = strSubstring;
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
