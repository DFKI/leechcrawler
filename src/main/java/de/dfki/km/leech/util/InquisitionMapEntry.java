package de.dfki.km.leech.util;



import java.io.Serializable;
import java.util.Map.Entry;



/**
 * An implementation of the Map.Entry interface
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 * @param <K>
 * @param <V>
 */
public class InquisitionMapEntry<K, V> implements Entry<K, V>, Serializable
{
    private static final long serialVersionUID = 7922114920330465804L;

    private K key;

    private V value;



    public InquisitionMapEntry(K key, V value)
    {
        this.key = key;
        this.value = value;
    }



    public InquisitionMapEntry()
    {
    }





    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj)
    {
        Entry<K, V> castedEntry = (Entry<K, V>) obj;

        if((this.getKey() == null ? castedEntry.getKey() == null : this.getKey().equals(castedEntry.getKey()))
                && (this.getValue() == null ? castedEntry.getValue() == null : this.getValue().equals(castedEntry.getValue()))) return true;

        return false;
    }



    @Override
    public K getKey()
    {
        return key;
    }



    @Override
    public V getValue()
    {
        return value;
    }



    public K setKey(K key)
    {
        K oldKey = this.key;
        this.key = key;

        return oldKey;
    }



    @Override
    public V setValue(V value)
    {
        V oldValue = this.value;
        this.value = value;

        return oldValue;
    }

}
