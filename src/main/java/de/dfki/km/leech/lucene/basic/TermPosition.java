package de.dfki.km.leech.lucene.basic;





public class TermPosition
{

    Integer m_iEndOffset;

    Integer m_iPosition;

    Integer m_iStartOffset;



    public Integer getEndOffset()
    {
        return m_iEndOffset;
    }



    public Integer getPosition()
    {
        return m_iPosition;
    }



    public Integer getStartOffset()
    {
        return m_iStartOffset;
    }



    public void setEndOffset(Integer endOffset)
    {
        m_iEndOffset = endOffset;
    }



    public void setPosition(Integer position)
    {
        m_iPosition = position;
    }



    public void setStartOffset(Integer startOffset)
    {
        m_iStartOffset = startOffset;
    }


}
