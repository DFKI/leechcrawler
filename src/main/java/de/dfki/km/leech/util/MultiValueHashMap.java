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

/*
 * Created on 08.07.2005
 */
package de.dfki.km.leech.util;



import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;



/**
 * HashMap that allows multiple values for each key. This is realized with Collections as HashMap values
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * @param <K> the key
 * @param <V> the value
 * 
 */
public class MultiValueHashMap<K, V> implements Serializable, MultiValueMap<K, V>
{

    private static final long serialVersionUID = -7898966371322704402L;




    /**
     * The internal collection that stores the real values as Collections
     */
    Map<K, Collection<V>> m_internalHashMap;




    int m_iValueCount = 0;



    /**
     * The type for the collections used as values inside the Map
     */
    @SuppressWarnings("rawtypes")
    Class m_valueCollectionType;



    /**
     * The value collection type becomes LinkedList.class as the default. There are other Constructors to specify this type (e.g. HashSet.class could
     * be of specific interest)
     * 
     */
    public MultiValueHashMap()
    {
        m_valueCollectionType = LinkedList.class;
        m_internalHashMap = createInternalMap(-1, -1, null);
    }



    /**
     * Constructor that allow the specification of the collection type that will be used to save the multiple values under a key. e.g.
     * MultiValueHashMap testMap = new MultiValueHashMap(LinkedList.class);
     * 
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     */
    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public MultiValueHashMap(Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalHashMap = createInternalMap(-1, -1, null);
    }



    /**
     * Constructs an empty <tt>MultiValueHashTable</tt> with the specified initial capacity and the default load factor (0.75).
     * 
     * @param initialCapacity the initial capacity.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public MultiValueHashMap(int initialCapacity, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalHashMap = createInternalMap(initialCapacity, -1, null);
    }



    /**
     * Constructs an empty <tt>MultiValueHashTable</tt> with the specified initial capacity and load factor.
     * 
     * @param initialCapacity The initial capacity.
     * @param loadFactor The load factor.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     * @throws IllegalArgumentException if the initial capacity is negative or the load factor is nonpositive.
     */
    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public MultiValueHashMap(int initialCapacity, float loadFactor, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalHashMap = createInternalMap(initialCapacity, loadFactor, null);
    }



    /**
     * Constructs a new <tt>MultiValueHashTable</tt> with the same mappings as the specified <tt>Map</tt>. The <tt>MultiValueHashTable</tt> is created
     * with default load factor (0.75) and an initial capacity sufficient to hold the mappings in the specified <tt>Map</tt>.
     * 
     * @param m the map whose mappings are to be placed in this map.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     * @throws NullPointerException if the specified map is null.
     */
    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public MultiValueHashMap(Map<K, Collection<V>> m, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalHashMap = createInternalMap(-1, -1, m);

        refreshValueCount();
    }



    /**
     * Constructs a new <tt>MultiValueHashTable</tt> with the same mappings as the specified <tt>Map</tt>. The <tt>MultiValueHashTable</tt> is created
     * with default load factor (0.75) and an initial capacity sufficient to hold the mappings in the specified <tt>Map</tt>.
     * 
     * @param m the map whose mappings are to be placed in this map.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     * @throws NullPointerException if the specified map is null.
     */
    @SuppressWarnings({ "unchecked", "cast", "rawtypes" })
    public MultiValueHashMap(MultiValueMap<K, V> m, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalHashMap = createInternalMap(-1, -1, null);

        this.addAll(m);
    }



    /**
     * Adds a value into the Collection under the specified key
     * 
     * @param key the key where to add the given value
     * @param value the value to add into the Collection under the specified key
     * 
     * @return null in the case the Collection changed as a result of the call, value otherwise. (Returns 'value' if this collection does not permit
     *         duplicates and already contains the specified element.)
     */
    @Override
    @SuppressWarnings("unchecked")
    public V add(K key, V value)
    {
        try
        {

            Collection<V> colValue = m_internalHashMap.get(key);

            if(colValue == null)
            {
                // sollte der key neu sein, muß eine neue Collection allokiert werden
                colValue = (Collection<V>) m_valueCollectionType.newInstance();

                m_internalHashMap.put(key, colValue);
            }

            if(colValue.add(value))
            {
                m_iValueCount++;
                return null;
            }

            return value;


        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }







    /**
     * Adds all values into the Collection under the specified key
     * 
     * @param key the key where to add the given values
     * @param values the values to add into the Collection under the specified key
     * 
     * @return null in the case the Collection changed as a result of the call, values otherwise. (Returns 'values' if this collection does not permit
     *         duplicates and already contains the specified element.)
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> addAll(K key, Collection<V> values)
    {
        try
        {


            Collection<V> colValue = m_internalHashMap.get(key);

            if(colValue == null)
            {
                // sollte der key neu sein, muß eine neue Collection allokiert werden
                colValue = (Collection<V>) m_valueCollectionType.newInstance();

                m_internalHashMap.put(key, colValue);
            }

            int iOldValueSize = colValue.size();

            if(colValue.addAll(values))
            {
                // falls die Collection ein Hashset ist, sind evtl. nur einige wenige eingetragen
                // worden - dann berechnen wir die neue Größe halt so :)
                m_iValueCount = m_iValueCount - iOldValueSize + values.size();
                return null;
            }

            return values;


        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }



    /**
     * Adds all values into the Collections under the specified keys in map
     * 
     * @param map a map with keys and according values to add into this map
     */
    @Override
    public void addAll(Map<K, V> map)
    {
        for (Iterator<Map.Entry<K, V>> itEntries = map.entrySet().iterator(); itEntries.hasNext();)
        {
            Map.Entry<K, V> entry = itEntries.next();

            this.add(entry.getKey(), entry.getValue());
        }
    }



    /**
     * Adds all values into the Collections under the specified keys in map
     * 
     * @param map a map with keys and according values to add into this map
     */
    @Override
    public void addAll(MultiValueMap<K, V> map)
    {
        for (Iterator<Map.Entry<K, V>> itEntries = map.entryList().iterator(); itEntries.hasNext();)
        {
            Map.Entry<K, V> entry = itEntries.next();

            this.add(entry.getKey(), entry.getValue());
        }
    }



    /**
     * Removes all mappings from this map.
     */
    @Override
    public void clear()
    {
        m_internalHashMap.clear();
        m_iValueCount = 0;
    }





    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @param key The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     */
    @Override
    public boolean containsKey(K key)
    {
        return m_internalHashMap.containsKey(key);
    }





    /**
     * Checks whether value is under the specified key or not
     * 
     * @param key the key that references the collection of interest
     * @param value the value to look for
     * 
     * @return true if value is inside the collection, false otherwise. false Can also mean that there is no mapping for key
     */
    @Override
    public boolean containsValue(K key, V value)
    {
        Collection<V> colValue = m_internalHashMap.get(key);

        if(colValue == null) return false;

        return colValue.contains(value);
    }





    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value.
     * 
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the specified value.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean containsValue(V value)
    {
        Set<Entry<K, Collection<V>>> entries = m_internalHashMap.entrySet();
        Iterator<Entry<K, Collection<V>>> itEntries = entries.iterator();

        while (itEntries.hasNext())
        {
            Entry<K, Collection<V>> entry = itEntries.next();
            // für jede Collection
            Collection colValue = entry.getValue();

            if(colValue.contains(value)) return true;
        }

        return false;
    }



    Map<K, Collection<V>> createInternalMap(int initialCapacity, float loadFactor, Map<K, Collection<V>> m)
    {
        Map<K, Collection<V>> map;

        if(initialCapacity < 0)
        {
            if(m == null)
                map = new HashMap<K, Collection<V>>();
            else
                map = new HashMap<K, Collection<V>>(m);
        }
        else if(loadFactor < 0)
            map = new HashMap<K, Collection<V>>(initialCapacity);
        else
            map = new HashMap<K, Collection<V>>(initialCapacity, loadFactor);



        return map;
    }



    /**
     * Returns a collection view of the mappings contained in this map. Each element in the returned collection is a <tt>Map.Entry</tt>. The
     * collection is NOT backed by the map, so changes to the map are NOT reflected in the collection, or vice-versa.
     * 
     * @return a collection view of the mappings contained in this map.
     * @see java.util.Map.Entry
     */
    @Override
    public List<Entry<K, V>> entryList()
    {
        LinkedList<Entry<K, V>> returnList = new LinkedList<Entry<K, V>>();

        for (K key : this.keySet())
            for (V value : this.get(key))
                returnList.add(new InquisitionMapEntry<K, V>(key, value));

        return returnList;
    }



    @SuppressWarnings({ "rawtypes" })
    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof MultiValueHashMap)) return false;

        return m_internalHashMap.equals(((MultiValueHashMap) o).m_internalHashMap);
    }



    /**
     * Gets all values under the specified key as a collection of the specified type
     * 
     * @param key the key of choice
     * 
     * @return all values under the specified key as a collection of the specified type
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> get(K key)
    {
        try
        {
            if(!m_internalHashMap.containsKey(key)) return (Collection<V>) m_valueCollectionType.newInstance();

            return m_internalHashMap.get(key);


        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }







    /**
     * Gets the first value under the specified key
     * 
     * @param key the key of choice
     * 
     * @return the first value under the specified key, null in the case there is no mapping for key, the Collection under key is empty or the first
     *         entry is a null entry
     */
    @Override
    public V getFirst(K key)
    {
        Collection<V> vals = this.get(key);
        if(vals == null) return null;
        if(vals.isEmpty()) return null;

        return vals.iterator().next();
    }



    Map<K, Collection<V>> getInternalHashMap()
    {
        return m_internalHashMap;
    }



    /**
     * Gets the count of occurences of value inside the collection under the specified key
     * 
     * @param key the key that specifies the collection of interest
     * @param value the value to count its occurences
     * 
     * @return the count of occurences of the value under the specified key, -1 if there is no mapping for key
     */
    @Override
    public int getValueCount(K key, V value)
    {

        Collection<V> colValue = m_internalHashMap.get(key);

        if(colValue == null) return -1;

        Iterator<V> itValues = colValue.iterator();

        int iCount = 0;
        while (itValues.hasNext())
        {
            V collectionValue = itValues.next();

            if(collectionValue.equals(value)) iCount++;
        }

        return iCount;
    }



    @Override
    public int hashCode()
    {
        return m_internalHashMap.hashCode();
    }



    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * 
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty()
    {
        return m_internalHashMap.isEmpty();
    }



    /**
     * Returns a set view of the keys contained in this map. The set is NOT backed by the map, so changes to the map are NOT reflected in the set, or
     * vice-versa.
     * 
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set<K> keySet()
    {
        return m_internalHashMap.keySet();
    }



    /**
     * Returns the number of keys in the Map
     * 
     * @return the number of keys
     */
    @Override
    public int keySize()
    {
        return m_internalHashMap.size();
    }



    /**
     * Refreshs the internal stored value size. Only necessary for internal reasons
     */
    void refreshValueCount()
    {
        int iValueCount = 0;
        for (K key : this.keySet())
            iValueCount += this.valueSize(key);

        m_iValueCount = iValueCount;
    }



    /**
     * Removes the mapping for this key from this map if present.
     * 
     * @param key key whose mapping is to be removed from the map.
     * @return the values that were previously value(s) associated with specified key, or <tt>null</tt> if there was no mapping for key. A
     *         <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the specified key, or that the collection
     *         was empty.
     */
    @Override
    public Collection<V> remove(K key)
    {
        Collection<V> colValue = m_internalHashMap.get(key);

        if(colValue != null) m_iValueCount -= colValue.size();

        Collection<V> colRemoved = m_internalHashMap.remove(key);
        if(colRemoved == null) return null;
        if(colRemoved.isEmpty()) return null;

        return colRemoved;
    }



    /**
     * Removes all occurences of the specified values in the Collection under the given key. If the underlying Collection goes empty the key inside
     * the map is also deleted
     * 
     * @param key the key which specifies the collection where to delete the occurences
     * @param values2Delete the values to delete under this key
     * 
     * @return true in the case the collection of values
     */
    @Override
    public boolean remove(K key, Collection<V> values2Delete)
    {
        if(values2Delete == null) return false;

        Iterator<V> itValues2Delete = values2Delete.iterator();
        boolean bValuesModified = false;

        while (itValues2Delete.hasNext())
        {
            if(this.remove(key, itValues2Delete.next()) != null) bValuesModified = true;
        }


        return bValuesModified;
    }



    /**
     * Removes all occurences of the specified value in the Collection under the given key. If the underlying Collection goes empty the key inside the
     * map is also deleted
     * 
     * @param key the key which specifies the collection where to delete the occurences
     * @param value the values to delete
     * 
     * @return value in the case there was an entry with value under the specified Collection, or null if there was no entry. A null return can also
     *         indicate that there was no mapping for the specified key or that value is null ;)
     */
    @Override
    public V remove(K key, V value)
    {
        Collection<V> colValues = m_internalHashMap.get(key);

        if(colValues == null) return null;

        boolean bColHasChanged = false;

        while (colValues.remove(value))
        {
            m_iValueCount--;
            bColHasChanged = true;
        }

        // wenn die Collection leer wird, wird der komplette eintrag gelöscht
        if(colValues.size() == 0) this.remove(key);

        if(bColHasChanged) return value;

        return null;
    }




    /**
     * Replaces all values under the specified key with the given value. The count of the values will not be modified.
     * 
     * @param key the key where to replace the values
     * @param value the new values
     * 
     * @return the former values under the specified key
     */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<V> replace(K key, V value)
    {
        try
        {
            // die Reihenfolge der keys sollte nicht geändert werden - hier nur die Value-Collections selbst bearbeiten

            Collection<V> colValue = this.get(key);

            Collection<V> colOldValue = (Collection<V>) m_valueCollectionType.newInstance();
            colOldValue.addAll(colValue);


            colValue.clear();

            // die Anzahl der values soll gleich bleiben - es werden ALLE value-werde mit dem Gegebenen ersetzt
            for (int i = 0; i < colOldValue.size(); i++)
                colValue.add(value);


            return colOldValue;


        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }



    /**
     * Replaces all values under a key with given values
     * 
     * @param key the key where to replace the old with the given values
     * @param values the new values
     * 
     * @return the former values under the specified key
     */
    @Override
    public Collection<V> replaceAll(K key, Collection<V> values)
    {
        Collection<V> colOldValue = this.get(key);

        this.remove(key);
        this.addAll(key, values);

        return colOldValue;
    }






    /**
     * Replaces all values under a set of keys with according values. This is equivalent to execute a replace(key, value) on all of the given map
     * antries.
     * 
     * @param map a map with keys and according values to replace in this map
     */
    @Override
    public void replaceAll(Map<K, V> map)
    {
        for (Iterator<Map.Entry<K, V>> itEntries = map.entrySet().iterator(); itEntries.hasNext();)
        {
            Map.Entry<K, V> entry = itEntries.next();

            this.replace(entry.getKey(), entry.getValue());
        }
    }



    /**
     * Replaces all values under a set of keys with according values. This is equivalent to execute a replace(key, value) on all of the given map
     * antries.
     * 
     * @param map a map with keys and according values to replace in this map
     */
    public void replaceAll(MultiValueMap<K, V> map)
    {
        for (Iterator<Map.Entry<K, V>> itEntries = map.entryList().iterator(); itEntries.hasNext();)
        {
            Map.Entry<K, V> entry = itEntries.next();

            this.replace(entry.getKey(), entry.getValue());
        }
    }



    @Override
    public String toString()
    {
        return m_internalHashMap.toString();
    }



    /**
     * Returns all Values from all value Collections inside one Collection (LinkedList)
     * 
     * @return all values inside this map
     */
    @Override
    public Collection<V> values()
    {
        Collection<V> col2Return = new LinkedList<V>();


        for (Iterator<? extends Map.Entry<K, Collection<V>>> itEntries = m_internalHashMap.entrySet().iterator(); itEntries.hasNext();)
        {
            Map.Entry<K, Collection<V>> entry = itEntries.next();

            Collection<V> colValue = entry.getValue();

            col2Return.addAll(colValue);
        }


        return col2Return;

    }



    /**
     * Returns the overall number of values inside all Collections
     * 
     * @return the overall number of values inside all Collections
     */
    @Override
    public int valueSize()
    {
        return m_iValueCount;
    }



    /**
     * Returns the number ob values under the specified key
     * 
     * @param key the key which specifies the collection of interest
     * 
     * @return the number of values under the specified key, -1 in the case there is no mapping for this key
     */
    @Override
    public int valueSize(K key)
    {
        Collection<V> colValues = m_internalHashMap.get(key);

        if(colValues == null) return -1;

        return colValues.size();
    }



}
