package de.dfki.km.leech.util;



import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.NavigableMap;
import java.util.UUID;

import org.apache.jdbm.DB;
import org.apache.jdbm.DBMaker;



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



    @Override
    protected Map createInternalMap(int initialCapacity, float loadFactor, Map m)
    {

        m_jdbmDB =
                DBMaker.openFile("multiValueBalancedTree_" + UUID.randomUUID().toString().replaceAll("\\W", "_")).disableTransactions()
                        .disableLocking().deleteFilesAfterClose().closeOnExit().make();


        NavigableMap<? extends Comparable, Collection<V>> map = m_jdbmDB.createTreeMap(UUID.randomUUID().toString().replaceAll("\\W", "_"));

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
