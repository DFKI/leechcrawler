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
