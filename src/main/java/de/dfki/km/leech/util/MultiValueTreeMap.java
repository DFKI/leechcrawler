/*
 * Created on 08.07.2005
 */
package de.dfki.km.leech.util;



import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;



/**
 * TreeMap that allows multiple values for each key. This is realized with Collections as TreeMap values. The MultiValueTreeMap is not prepared for
 * xmlRPC conversion becauese it is not such a good idea to pick the TreeMap as a data structure for communication - at least in the case when you
 * deal with custom comparators.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * @param <K> the key
 * @param <V> the value
 * 
 */
public class MultiValueTreeMap<K, V> implements Serializable, MultiValueMap<K, V>
{

    private static final long serialVersionUID = -7672911262867737336L;




    /**
     * The internal collection that stores the real values as Collections
     */
    private TreeMap<K, Collection<V>> m_internalTreeMap;

    private int m_iValueCount = 0;

    /**
     * The type for the collections used as values inside the Map
     */
    private Class<Collection<V>> m_valueCollectionType;



    /**
     * Constructor that allow the specification of the collection type that will be used to save the multiple values under a key. e.g.
     * MultiValueHashMap testMap = new MultiValueHashMap(new LinkedList().getClass());
     * 
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     */
    @SuppressWarnings("unchecked")
    public MultiValueTreeMap(Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalTreeMap = new TreeMap<K, Collection<V>>();
    }



    /**
     * Constructs a new, empty map, sorted according to the given comparator. All keys inserted into the map must be <i>mutually comparable</i> by the
     * given comparator: <tt>comparator.compare(k1, k2)</tt> must not throw a <tt>ClassCastException</tt> for any keys <tt>k1</tt> and <tt>k2</tt> in
     * the map. If the user attempts to put a key into the map that violates this constraint, the <tt>put(Object key, Object
     * value)</tt> call will throw a <tt>ClassCastException</tt>.
     * 
     * @param c the comparator that will be used to sort this map. A <tt>null</tt> value indicates that the keys' <i>natural ordering</i> should be
     *            used.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     */
    @SuppressWarnings("unchecked")
    public MultiValueTreeMap(Comparator<? super K> c, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalTreeMap = new TreeMap<K, Collection<V>>(c);
    }




    /**
     * Constructs a new map containing the same mappings as the given map, sorted according to the keys' <i>natural order</i>. All keys inserted into
     * the new map must implement the <tt>Comparable</tt> interface. Furthermore, all such keys must be <i>mutually comparable</i>:
     * <tt>k1.compareTo(k2)</tt> must not throw a <tt>ClassCastException</tt> for any elements <tt>k1</tt> and <tt>k2</tt> in the map. This method
     * runs in n*log(n) time.
     * 
     * @param m the map whose mappings are to be placed in this map.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     * @throws ClassCastException the keys in t are not Comparable, or are not mutually comparable.
     * @throws NullPointerException if the specified map is null.
     */
    @SuppressWarnings("unchecked")
    public MultiValueTreeMap(Map<K, Collection<V>> m, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalTreeMap = new TreeMap<K, Collection<V>>(m);

        refreshValueCount();
    }



    /**
     * Constructs a new map containing the same mappings as the given map, sorted according to the keys' <i>natural order</i>. All keys inserted into
     * the new map must implement the <tt>Comparable</tt> interface. Furthermore, all such keys must be <i>mutually comparable</i>:
     * <tt>k1.compareTo(k2)</tt> must not throw a <tt>ClassCastException</tt> for any elements <tt>k1</tt> and <tt>k2</tt> in the map. This method
     * runs in n*log(n) time.
     * 
     * @param m the map whose mappings are to be placed in this map.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     * @throws ClassCastException the keys in t are not Comparable, or are not mutually comparable.
     * @throws NullPointerException if the specified map is null.
     */
    @SuppressWarnings("unchecked")
    public MultiValueTreeMap(MultiValueMap<K, V> m, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalTreeMap = new TreeMap<K, Collection<V>>();

        this.addAll(m);
    }




    /**
     * Constructs a new map containing the same mappings as the given <tt>SortedMap</tt>, sorted according to the same ordering. This method runs in
     * linear time.
     * 
     * @param m the sorted map whose mappings are to be placed in this map, and whose comparator is to be used to sort this map.
     * @param valueCollectionType specifies the collection type that will be used for the values ( e.g. LinkedList.class )
     * @throws NullPointerException if the specified sorted map is null.
     */
    @SuppressWarnings("unchecked")
    public MultiValueTreeMap(SortedMap<K, Collection<V>> m, Class<? extends Collection> valueCollectionType)
    {
        m_valueCollectionType = (Class<Collection<V>>) valueCollectionType;
        m_internalTreeMap = new TreeMap<K, Collection<V>>(m);

        refreshValueCount();
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
    public V add(K key, V value)
    {
        try
        {

            Collection<V> colValue = m_internalTreeMap.get(key);

            if(colValue == null)
            {
                // sollte der key neu sein, muß eine neue Collection allokiert werden
                colValue = m_valueCollectionType.newInstance();

                m_internalTreeMap.put(key, colValue);
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
    public Collection<V> addAll(K key, Collection<V> values)
    {
        try
        {


            Collection<V> colValue = m_internalTreeMap.get(key);

            if(colValue == null)
            {
                // sollte der key neu sein, muß eine neue Collection allokiert werden
                colValue = m_valueCollectionType.newInstance();

                m_internalTreeMap.put(key, colValue);
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
    public void clear()
    {
        m_internalTreeMap.clear();
        m_iValueCount = 0;
    }



    /**
     * Returns a shallow copy of this <tt>HashMap</tt> instance: the keys and values themselves are not cloned.
     * 
     * @return a shallow copy of this map.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Object clone()
    {
        MultiValueTreeMap<K, V> result = new MultiValueTreeMap<K, V>(m_valueCollectionType);

        result.m_internalTreeMap = (TreeMap<K, Collection<V>>) this.m_internalTreeMap.clone();
        result.m_iValueCount = this.m_iValueCount;

        return result;
    }



    /**
     * Returns the comparator used to order this map, or <tt>null</tt> if this map uses its keys' natural order.
     * 
     * @return the comparator associated with this sorted map, or <tt>null</tt> if it uses its keys' natural sort method.
     */
    @SuppressWarnings("unchecked")
    public Comparator comparator()
    {
        return m_internalTreeMap.comparator();
    }



    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @param key The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     */
    public boolean containsKey(K key)
    {
        return m_internalTreeMap.containsKey(key);
    }



    /**
     * Checks whether value is under the specified key or not
     * 
     * @param key the key that references the collection of interest
     * @param value the value to look for
     * 
     * @return true if value is inside the collection, false otherwise. false Can also mean that there is no mapping for key
     */
    public boolean containsValue(K key, V value)
    {
        Collection<V> colValue = m_internalTreeMap.get(key);

        if(colValue == null) return false;

        return colValue.contains(value);
    }




    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value.
     * 
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the specified value.
     */
    @SuppressWarnings("unchecked")
    public boolean containsValue(V value)
    {
        Set<Entry<K, Collection<V>>> entries = m_internalTreeMap.entrySet();
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



    /**
     * Returns a collection view of the mappings contained in this map. Each element in the returned collection is a <tt>Map.Entry</tt>. The
     * collection is NOT backed by the map, so changes to the map are NOT reflected in the collection, or vice-versa.
     * 
     * @return a collection view of the mappings contained in this map.
     * @see Map.Entry
     */
    public List<Entry<K, V>> entryList()
    {
        LinkedList<Entry<K, V>> returnList = new LinkedList<Entry<K, V>>();

        for (K key : this.keySet())
            for (V value : this.get(key))
                returnList.add(new InquisitionMapEntry<K, V>(key, value));

        return returnList;
    }






    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o)
    {
        if(!(o instanceof MultiValueTreeMap)) return false;

        return m_internalTreeMap.equals(((MultiValueTreeMap) o).m_internalTreeMap);
    }




    /**
     * Returns the first (lowest) key currently in this sorted map.
     * 
     * @return the first (lowest) key currently in this sorted map.
     * @throws NoSuchElementException Map is empty.
     */
    public K firstKey()
    {
        return m_internalTreeMap.firstKey();
    }




    /**
     * Gets all values under the specified key as a collection of the specified type
     * 
     * @param key the key of choice
     * 
     * @return all values under the specified key as a collection of the specified type
     */
    public Collection<V> get(K key)
    {
        try
        {
            if(!m_internalTreeMap.containsKey(key)) return m_valueCollectionType.newInstance();


            return m_internalTreeMap.get(key);

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
    public V getFirst(K key)
    {
        Collection<V> vals = this.get(key);
        if(vals == null) return null;
        if(vals.isEmpty()) return null;

        return vals.iterator().next();
    }




    /**
     * Gets the count of occurences of value inside the collection under the specified key
     * 
     * @param key the key that specifies the collection of interest
     * @param value the value to count its occurences
     * 
     * @return the count of occurences of the value under the specified key, -1 if there is no mapping for key
     */
    public int getValueCount(K key, V value)
    {

        Collection<V> colValue = m_internalTreeMap.get(key);

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
        return m_internalTreeMap.hashCode();
    }




    /**
     * Returns a view of the portion of this map whose keys are strictly less than <tt>toKey</tt>. The returned sorted map is backed by this map, so
     * changes in the returned sorted map are reflected in this map, and vice-versa. The returned sorted map supports all optional map operations.
     * <p>
     * 
     * The sorted map returned by this method will throw an <tt>IllegalArgumentException</tt> if the user attempts to insert a key greater than or
     * equal to <tt>toKey</tt>.
     * <p>
     * 
     * Note: this method always returns a view that does not contain its (high) endpoint. If you need a view that does contain this endpoint, and the
     * key type allows for calculation of the successor a given key, merely request a headMap bounded by <tt>successor(highEndpoint)</tt>. For
     * example, suppose that suppose that <tt>m</tt> is a sorted map whose keys are strings. The following idiom obtains a view containing all of the
     * key-value mappings in <tt>m</tt> whose keys are less than or equal to <tt>high</tt>:
     * 
     * <pre>
     * SortedMap head = m.headMap(high + &quot;\0&quot;);
     * </pre>
     * 
     * @param toKey high endpoint (exclusive) of the headMap.
     * @return a view of the portion of this map whose keys are strictly less than <tt>toKey</tt>.
     * 
     * @throws ClassCastException if <tt>toKey</tt> is not compatible with this map's comparator (or, if the map has no comparator, if <tt>toKey</tt>
     *             does not implement <tt>Comparable</tt>).
     * @throws IllegalArgumentException if this map is itself a subMap, headMap, or tailMap, and <tt>toKey</tt> is not within the specified range of
     *             the subMap, headMap, or tailMap.
     * @throws NullPointerException if <tt>toKey</tt> is <tt>null</tt> and this map uses natural order, or its comparator does not tolerate
     *             <tt>null</tt> keys.
     */
    public SortedMap<K, Collection<V>> headMap(K toKey)
    {
        return m_internalTreeMap.headMap(toKey);
    }



    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * 
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty()
    {
        return m_internalTreeMap.isEmpty();
    }



    /**
     * Returns a set view of the keys contained in this map. The set is NOT backed by the map, so changes to the map are NOT reflected in the set, or
     * vice-versa.
     * 
     * @return a set view of the keys contained in this map.
     */
    public Set<K> keySet()
    {
        return m_internalTreeMap.keySet();
    }



    /**
     * Returns the number of keys in the Map
     * 
     * @return the number of keys
     */
    public int keySize()
    {
        return m_internalTreeMap.size();
    }



    /**
     * Returns the last (highest) key currently in this sorted map.
     * 
     * @return the last (highest) key currently in this sorted map.
     * @throws NoSuchElementException Map is empty.
     */
    public K lastKey()
    {
        return m_internalTreeMap.lastKey();
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
     * @return the value(s) that were previously value(s) associated with specified key, or <tt>null</tt> if there was no mapping for key. A
     *         <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the specified key, or that the collection
     *         was empty.
     */
    public Collection<V> remove(K key)
    {
        Collection<V> colValue = m_internalTreeMap.get(key);

        if(colValue != null) m_iValueCount -= colValue.size();

        Collection<V> colRemoved = m_internalTreeMap.remove(key);
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
    public V remove(K key, V value)
    {
        Collection<V> colValues = m_internalTreeMap.get(key);

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
    public Collection<V> replace(K key, V value)
    {
        try
        {
            // die Reihenfolge der keys sollte nicht geändert werden - hier nur die Value-Collections selbst bearbeiten

            Collection<V> colValue = this.get(key);

            Collection<V> colOldValue = m_valueCollectionType.newInstance();
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




    /**
     * Returns a view of the portion of this map whose keys range from <tt>fromKey</tt>, inclusive, to <tt>toKey</tt>, exclusive. (If <tt>fromKey</tt>
     * and <tt>toKey</tt> are equal, the returned sorted map is empty.) The returned sorted map is backed by this map, so changes in the returned
     * sorted map are reflected in this map, and vice-versa. The returned sorted map supports all optional map operations.
     * <p>
     * 
     * The sorted map returned by this method will throw an <tt>IllegalArgumentException</tt> if the user attempts to insert a key less than
     * <tt>fromKey</tt> or greater than or equal to <tt>toKey</tt>.
     * <p>
     * 
     * Note: this method always returns a <i>half-open range</i> (which includes its low endpoint but not its high endpoint). If you need a <i>closed
     * range</i> (which includes both endpoints), and the key type allows for calculation of the successor a given key, merely request the subrange
     * from <tt>lowEndpoint</tt> to <tt>successor(highEndpoint)</tt>. For example, suppose that <tt>m</tt> is a sorted map whose keys are strings. The
     * following idiom obtains a view containing all of the key-value mappings in <tt>m</tt> whose keys are between <tt>low</tt> and <tt>high</tt>,
     * inclusive:
     * 
     * <pre>
     * SortedMap sub = m.submap(low, high + &quot;\0&quot;);
     * </pre>
     * 
     * A similar technique can be used to generate an <i>open range</i> (which contains neither endpoint). The following idiom obtains a view
     * containing all of the key-value mappings in <tt>m</tt> whose keys are between <tt>low</tt> and <tt>high</tt>, exclusive:
     * 
     * <pre>
     * SortedMap sub = m.subMap(low + &quot;\0&quot;, high);
     * </pre>
     * 
     * @param fromKey low endpoint (inclusive) of the subMap.
     * @param toKey high endpoint (exclusive) of the subMap.
     * 
     * @return a view of the portion of this map whose keys range from <tt>fromKey</tt>, inclusive, to <tt>toKey</tt>, exclusive.
     * 
     * @throws ClassCastException if <tt>fromKey</tt> and <tt>toKey</tt> cannot be compared to one another using this map's comparator (or, if the map
     *             has no comparator, using natural ordering).
     * @throws IllegalArgumentException if <tt>fromKey</tt> is greater than <tt>toKey</tt>.
     * @throws NullPointerException if <tt>fromKey</tt> or <tt>toKey</tt> is <tt>null</tt> and this map uses natural order, or its comparator does not
     *             tolerate <tt>null</tt> keys.
     */
    public SortedMap<K, Collection<V>> subMap(K fromKey, K toKey)
    {
        return m_internalTreeMap.subMap(fromKey, toKey);
    }







    /**
     * Returns a view of the portion of this map whose keys are greater than or equal to <tt>fromKey</tt>. The returned sorted map is backed by this
     * map, so changes in the returned sorted map are reflected in this map, and vice-versa. The returned sorted map supports all optional map
     * operations.
     * <p>
     * 
     * The sorted map returned by this method will throw an <tt>IllegalArgumentException</tt> if the user attempts to insert a key less than
     * <tt>fromKey</tt>.
     * <p>
     * 
     * Note: this method always returns a view that contains its (low) endpoint. If you need a view that does not contain this endpoint, and the
     * element type allows for calculation of the successor a given value, merely request a tailMap bounded by <tt>successor(lowEndpoint)</tt>. For
     * example, suppose that <tt>m</tt> is a sorted map whose keys are strings. The following idiom obtains a view containing all of the key-value
     * mappings in <tt>m</tt> whose keys are strictly greater than <tt>low</tt>:
     * 
     * <pre>
     * SortedMap tail = m.tailMap(low + &quot;\0&quot;);
     * </pre>
     * 
     * @param fromKey low endpoint (inclusive) of the tailMap.
     * @return a view of the portion of this map whose keys are greater than or equal to <tt>fromKey</tt>.
     * @throws ClassCastException if <tt>fromKey</tt> is not compatible with this map's comparator (or, if the map has no comparator, if
     *             <tt>fromKey</tt> does not implement <tt>Comparable</tt>).
     * @throws IllegalArgumentException if this map is itself a subMap, headMap, or tailMap, and <tt>fromKey</tt> is not within the specified range of
     *             the subMap, headMap, or tailMap.
     * @throws NullPointerException if <tt>fromKey</tt> is <tt>null</tt> and this map uses natural order, or its comparator does not tolerate
     *             <tt>null</tt> keys.
     */
    public SortedMap<K, Collection<V>> tailMap(K fromKey)
    {
        return m_internalTreeMap.tailMap(fromKey);
    }



    @Override
    public String toString()
    {
        return m_internalTreeMap.toString();
    }



    /**
     * Returns all Values from all value Collections inside one Collection (LinkedList)
     * 
     * @return all values inside this map
     */
    public Collection<V> values()
    {
        Collection<V> col2Return = new LinkedList<V>();


        for (Iterator<? extends Map.Entry<K, Collection<V>>> itEntries = m_internalTreeMap.entrySet().iterator(); itEntries.hasNext();)
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
    public int valueSize(K key)
    {
        Collection<V> colValues = m_internalTreeMap.get(key);

        if(colValues == null) return -1;

        return colValues.size();
    }



}
