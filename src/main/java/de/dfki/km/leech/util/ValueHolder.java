package de.dfki.km.leech.util;


/**
 * A simple class for holding a value for call by reference invocations
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 *
 * @param <T>
 */
public class ValueHolder<T>
{
    public T value = null;



    public ValueHolder()
    {
    }



    public ValueHolder(T value)
    {
        this.value = value;
    }



    public T getValue()
    {
        return value;
    }



    public void setValue(T value)
    {
        this.value = value;
    }
}
