package de.dfki.km.leech.util;



import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import org.mapdb.DB;
import org.mapdb.DBMaker;



public class MultiValueBalancedTreeMap<K, V> extends MultiValueHashMap<K, V>
{



    public static void main(String[] args)
    {

        MultiValueBalancedTreeMap<String, String> hsBla = new MultiValueBalancedTreeMap<String, String>(LinkedList.class);


        for (int i = 0; i < 1000000; i++)
        {
            hsBla.add(Integer.toString(i), "matcha");


            if(i % 1000 == 0)
            {
                hsBla.add(Integer.toString(i), "matchavariationen");
                hsBla.add(Integer.toString(i), "matcha mit Milch");
                hsBla.add(Integer.toString(i), "matcha mit Milch und Süßstoff");
                System.out.println(i);
            }
        }

        System.out.println("was ist lecker, mit 3 Einträgen: " + hsBla.get(Integer.toString(6000)));
        System.out.println("der zweite davon: " + hsBla.get(Integer.toString(6000)).toArray()[1]);
        System.out.println("was ist lecker, mit 1 Eintrag: " + hsBla.get(Integer.toString(5999)));

    }







    protected DB m_jdbmDB;



    public MultiValueBalancedTreeMap()
    {
        super();
    }



    public MultiValueBalancedTreeMap(Class valueCollectionType)
    {
        super(valueCollectionType);
    }



    public MultiValueBalancedTreeMap(int initialCapacity, Class valueCollectionType)
    {
        super(initialCapacity, valueCollectionType);
    }



    public MultiValueBalancedTreeMap(int initialCapacity, float loadFactor, Class valueCollectionType)
    {
        super(initialCapacity, loadFactor, valueCollectionType);
    }



    public MultiValueBalancedTreeMap(Map m, Class valueCollectionType)
    {
        super(m, valueCollectionType);
    }



    public MultiValueBalancedTreeMap(MultiValueMap m, Class valueCollectionType)
    {
        super(m, valueCollectionType);
    };




    /**
     * Adds a value into the Collection under the specified key. Must be overwritten because of a bug inside jdbm (some values are lost if you don't
     * put the value again)
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

            // sollte der key neu sein, muß eine neue Collection allokiert werden
            if(colValue == null) colValue = (Collection<V>) m_valueCollectionType.newInstance();

            if(colValue.add(value))
            {
                // for JDBM-bug: we put the collection again, independently it was inside the map before. This is not necessary with memory-backed
                // models
                m_internalHashMap.put(key, colValue);

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
     * Adds all values into the Collection under the specified key. Must be overwritten because of a bug inside jdbm (some values are lost if you
     * don't put the value again)
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

            // sollte der key neu sein, muß eine neue Collection allokiert werden
            if(colValue == null) colValue = (Collection<V>) m_valueCollectionType.newInstance();

            int iOldValueSize = colValue.size();

            if(colValue.addAll(values))
            {
                // for JDBM-bug: we put the collection again, independently it was inside the map before. This is not necessary with memory-backed
                // models
                m_internalHashMap.put(key, colValue);

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



    @Override
    protected Map createInternalMap(int initialCapacity, float loadFactor, Map m)
    {

        // JDBM3
        // m_jdbmDB =
        // DBMaker.openFile("multiValueBalancedTree_" + UUID.randomUUID().toString().replaceAll("\\W", "_")).disableTransactions()
        // .disableLocking().deleteFilesAfterClose().closeOnExit().make();
        // NavigableMap<? extends Comparable, Collection<V>> map = m_jdbmDB.createTreeMap(UUID.randomUUID().toString().replaceAll("\\W", "_"));

        m_jdbmDB = DBMaker.newTempFileDB().deleteFilesAfterClose().closeOnJvmShutdown().journalDisable().asyncWriteDisable().make();

        Map<? extends Comparable, Collection<V>> map = m_jdbmDB.getHashMap("temp");


        if(m != null) map.putAll(m);


        return map;
    }



    @Override
    protected void finalize() throws Throwable
    {
        if(m_jdbmDB != null)
        {
            m_jdbmDB.close();
            m_jdbmDB = null;
        }
    }



}
