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



import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;



/**
 * Interface for multi value maps.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * @param <K> the key
 * @param <V> the value
 * 
 */
public interface MultiValueMap<K, V>
{





    /**
     * Adds a value into the Collection under the specified key
     * 
     * @param key the key where to add the given value
     * @param value the value to add into the Collection under the specified key
     * 
     * @return null in the case the Collection changed as a result of the call, value otherwise. (Returns 'value' if this collection does not permit
     *         duplicates and already contains the specified element.)
     */
    public V add(K key, V value);



    /**
     * Adds all values into the Collection under the specified key
     * 
     * @param key the key where to add the given values
     * @param values the values to add into the Collection under the specified key
     * 
     * @return null in the case the Collection changed as a result of the call, values otherwise. (Returns 'values' if this collection does not permit
     *         duplicates and already contains the specified element.)
     */
    public Collection<V> addAll(K key, Collection<V> values);



    /**
     * Adds all values into the Collections under the specified keys in map
     * 
     * @param map a map with keys and according values to add into this map
     */
    public void addAll(Map<K, V> map);



    /**
     * Adds all values into the Collections under the specified keys in map
     * 
     * @param map a map with keys and according values to add into this map
     */
    public void addAll(MultiValueMap<K, V> map);




    /**
     * Removes all mappings from this map.
     */
    public void clear();





    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     * 
     * @param key The key whose presence in this map is to be tested
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     */
    public boolean containsKey(K key);



    /**
     * Checks whether value is under the specified key or not
     * 
     * @param key the key that references the collection of interest
     * @param value the value to look for
     * 
     * @return true if value is inside the collection, false otherwise. false Can also mean that there is no mapping for key
     */
    public boolean containsValue(K key, V value);



    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the specified value.
     * 
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the specified value.
     */
    public boolean containsValue(V value);




    /**
     * Returns a collection view of the mappings contained in this map. Each element in the returned collection is a <tt>Map.Entry</tt>. The
     * collection is NOT backed by the map, so changes to the map are NOT reflected in the collection, or vice-versa.
     * 
     * @return a collection view of the mappings contained in this map.
     * @see java.util.Map.Entry
     */
    public List<Entry<K, V>> entryList();




    /**
     * Gets all values under the specified key as a collection of the specified type
     * 
     * @param key the key of choice
     * 
     * @return all values under the specified key as a collection of the specified type
     */
    public Collection<V> get(K key);





    /**
     * Gets the first value under the specified key
     * 
     * @param key the key of choice
     * 
     * @return the first value under the specified key, null in the case there is no mapping for key, the Collection under key is empty or the first
     *         entry is a null entry
     */
    public V getFirst(K key);



    /**
     * Gets the count of occurences of value inside the collection under the specified key
     * 
     * @param key the key that specifies the collection of interest
     * @param value the value to count its occurences
     * 
     * @return the count of occurences of the value under the specified key, -1 if there is no mapping for key
     */
    public int getValueCount(K key, V value);




    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * 
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    public boolean isEmpty();



    /**
     * Returns a set view of the keys contained in this map. The set is NOT backed by the map, so changes to the map are NOT reflected in the set, or
     * vice-versa.
     * 
     * @return a set view of the keys contained in this map.
     */
    public Set<K> keySet();



    /**
     * Returns the number of keys in the Map
     * 
     * @return the number of keys
     */
    public int keySize();




    /**
     * Removes the mapping for this key from this map if present.
     * 
     * @param key key whose mapping is to be removed from the map.
     * @return the values that were previously value(s) associated with specified key, or <tt>null</tt> if there was no mapping for key. A
     *         <tt>null</tt> return can also indicate that the map previously associated <tt>null</tt> with the specified key, or that the collection
     *         was empty.
     */
    public Collection<V> remove(K key);




    /**
     * Removes all occurences of the specified values in the Collection under the given key. If the underlying Collection goes empty the key inside
     * the map is also deleted
     * 
     * @param key the key which specifies the collection where to delete the occurences
     * @param values2Delete the values to delete under this key
     * 
     * @return true in the case the collection of values
     */
    public boolean remove(K key, Collection<V> values2Delete);




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
    public V remove(K key, V value);




    /**
     * Replaces all values under the specified key with the given value. The count of the values will not be modified.
     * 
     * @param key the key where to replace the values
     * @param value the new values
     * 
     * @return the former values under the specified key
     */
    public Collection<V> replace(K key, V value);




    /**
     * Replaces all values under a key with given values
     * 
     * @param key the key where to replace the old with the given values
     * @param values the new values
     * 
     * @return the former values under the specified key
     */
    public Collection<V> replaceAll(K key, Collection<V> values);



    /**
     * Replaces all values under a set of keys with according values. This is equivalent to execute a replace(key, value) on all of the given map
     * antries.
     * 
     * @param map a map with keys and according values to replace in this map
     */
    public void replaceAll(Map<K, V> map);





    /**
     * Returns all Values from all value Collections inside one Collection (LinkedList)
     * 
     * @return all values inside this map
     */
    public Collection<V> values();




    /**
     * Returns the overall number of values inside all Collections
     * 
     * @return the overall number of values inside all Collections
     */
    public int valueSize();



    /**
     * Returns the number ob values under the specified key
     * 
     * @param key the key which specifies the collection of interest
     * 
     * @return the number of values under the specified key, -1 in the case there is no mapping for this key
     */
    public int valueSize(K key);







}
