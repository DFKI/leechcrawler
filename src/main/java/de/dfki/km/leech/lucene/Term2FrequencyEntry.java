package de.dfki.km.leech.lucene;



public class Term2FrequencyEntry
{

    String m_strTerm;

    Integer m_iFrequency;



    public Term2FrequencyEntry()
    {
    }



    public Term2FrequencyEntry(String strTerm, Integer iFrequency)
    {
        m_strTerm = strTerm;
        m_iFrequency = iFrequency;

    }



    public String getTerm()
    {
        return m_strTerm;
    }



    public void setTerm(String term)
    {
        m_strTerm = term;
    }



    public Integer getFrequency()
    {
        return m_iFrequency;
    }



    public void setFrequency(Integer frequency)
    {
        m_iFrequency = frequency;
    }



    @Override
    public String toString()
    {
        return "Term:" + getTerm() + " FRQ:" + getFrequency();
    }

}
