package de.dfki.km.leech.util;

public class LeechException extends Exception
{
    private static final long serialVersionUID = 7184624329588842248L;

    public LeechException()
    {
        super();
    }

    public LeechException(String message)
    {
        super(message);
    }



    public LeechException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public LeechException(Throwable cause)
    {
        super(cause);
    }

}
