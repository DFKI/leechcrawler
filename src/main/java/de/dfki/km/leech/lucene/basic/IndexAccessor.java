package de.dfki.km.leech.lucene.basic;



import de.dfki.inquisitor.exceptions.ExceptionUtils;
import de.dfki.inquisitor.logging.LoggingUtils;
import de.dfki.inquisitor.text.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;



@SuppressWarnings({"JavaDoc", "PointlessBooleanExpression"})
public class IndexAccessor
{

    public static class BetterMultiReader extends MultiReader
    {


        public BetterMultiReader(IndexReader... subReaders) throws IOException
        {
            super(subReaders);
        }



        public BetterMultiReader(IndexReader[] subReaders, boolean closeSubReaders) throws IOException
        {
            super(subReaders, closeSubReaders);
        }



        public List<? extends IndexReader> getSubReaders()
        {
            return getSequentialSubReaders();
        }
    }




    /**
     * Status constants for removeReaderFromCacheWhenPossible
     * 
     * @author Christian Reuschling, Dipl.Ing.(BA)
     */
    public static enum ReaderStatus {
        READER_CLOSED, READER_IN_QUEUE, READER_NOT_IN_CACHE;
    }




    protected static class ReaderRefreshRunnable implements Runnable
    {

        @Override
        public void run()
        {

            try
            {
                while (true)
                {

                    // wir warten das eingestellte Intervall

                    // ich hatte mal die Situation, daß der Thread nur im korrekten Intervall ausgeführt wird, wenn hier vor dem Sleep noch eine
                    // Ausgabe steht - da das eigentlich nicht sein kann, und das nur zum debuggen relevant war, mach ich das mal wieder weg. Er kam
                    // dann, aber halt nicht so oft. Aber schon innerhalb 2min (und nicht 10ms, wie ich es da wollte)
                    // LinkedList<String> dummy = new LinkedList<String>();
                    // System.err.print(".");
                    Thread.sleep(m_lReaderRefreshIntervall);

                    Logger.getLogger(this.getClass().getName()).fine("will refresh all index readers");

                    IndexAccessor.refreshAllIndexReaders();
                }

            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }


    private static String m_strIdAttributeName;

    protected static Logger logger = Logger.getLogger(IndexAccessor.class.getName());

    protected static Analyzer m_analyzer4writer;

    // protected static boolean m_bNativeFileLock = true;

    protected static HashMap<String, IndexReader> m_hsIndexPathOrId2CurrentIndexReader = new HashMap<String, IndexReader>();

    // protected static HashMap<String, RemoteIndexSearcher> m_hsIndexPathOrURL2CurrentRemoteSearcher = new HashMap<String, RemoteIndexSearcher>();

    // wenn man mehrere Instanzen von luceneIndexSet hat, darf trotzdem nur ein Writer pro Index offen sein
    protected static HashMap<String, IndexWriter> m_hsIndexPathOrURL2Writer = new HashMap<String, IndexWriter>();

    protected static HashMap<IndexReader, String> m_hsIndexReader2IndexPath = new HashMap<IndexReader, String>();

    protected static HashMap<IndexReader, Integer> m_hsIndexReader2ReaderRefCount = new HashMap<IndexReader, Integer>();


    protected static HashMap<IndexWriter, Integer> m_hsIndexWriter2WriterRefCount = new HashMap<IndexWriter, Integer>();






    protected static HashSet<IndexReader> m_hsReader2Remove = new HashSet<IndexReader>();



    protected static HashSet<IndexReader> m_hsStaticIndexReaderSet = new HashSet<IndexReader>();





    protected static long m_lReaderRefreshIntervall = 1000 * 60 * 2;



    static
    {

        try
        {



            // wir starten den Thread, der die reader objekte refreshed

            Thread readerRefreshThread = new Thread(new ReaderRefreshRunnable(), "IndexAccessor reader refresh thread");
            readerRefreshThread.setDaemon(true);
            // welche Priority? ich hatte mal das Gefühl, daß der recht selten dran kommt
            // readerRefreshThread.setPriority(Thread.MIN_PRIORITY);
            // readerRefreshThread.setPriority(Thread.MAX_PRIORITY);
            readerRefreshThread.start();



            // ein shutdown hook um sicherzustellen, daß auch alle Objekte geschlossen werden - wir wollen ja keine anderen Prozesse blockieren

            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        IndexAccessor.forceCloseAll();
                    }
                    catch (Exception ex)
                    {
                        throw new RuntimeException(ex);
                    }
                }
            });


        }
        catch (Exception e)
        {
            Logger.getLogger(IndexAccessor.class.getName()).log(Level.SEVERE, "Error", e);
        }

    }




    /**
     * Adds a reader object to the cache. This reader will be static, which means that it won't be refreshed in any case, independent of which method you invoke on
     * {@link IndexAccessor}, nor in the refresh-Thread. You can get this reader with {@link #getLuceneIndexReader(String, boolean)}, with strIndexID as parameter.You also can remove
     * the reader from cache with {@link #removeReaderFromCache(String)}, {@link #removeReaderFromCacheWhenPossible(String)} and {@link #removeUnusedReadersFromCache()}
     * 
     * 
     * @param strIndexID a unique ID for the reader
     * @param staticReader the reader Object
     */
    synchronized static public void addStaticReader(String strIndexID, IndexReader staticReader)
    {
        // wir merken uns den Reader, damit wir ihn nicht später aus Versehen ersetzen/refreshen
        m_hsStaticIndexReaderSet.add(staticReader);

        // und mit seiner ID kommt er auch noch in den Cache
        m_hsIndexPathOrId2CurrentIndexReader.put(strIndexID, staticReader);
    }



    /**
     * Creates a new, empty Lucene index under the given path
     * 
     * @param strIndexPathOrURL the path for the new Lucene index. In the case the path does not exists, it will be created
     * @param bForceAndOverwrite if this is false, the index will be only created in the case there is no existing index under strIndexPathOrURL
     * 
     * @return true in the case the index was newly created, false otherwise. In the case strIndexPathOrURL exists and is a file, it will not created in any case
     * 
     * @throws IOException
     * @throws CorruptIndexException
     */
    synchronized static public boolean createNewIndex(String strIndexPathOrURL, boolean bForceAndOverwrite) throws CorruptIndexException, IOException
    {
        boolean bCreateNew = false;

        File fIndexPath = new File(strIndexPathOrURL);

        if(!fIndexPath.exists())
        {
            fIndexPath.mkdirs();

            bCreateNew = true;
        }

        FSDirectory dir = createFSDirectory(fIndexPath);

        if(bCreateNew == false && (!DirectoryReader.indexExists(dir) || bForceAndOverwrite))
        {
            bCreateNew = true;
        }

        if(!bCreateNew) return false;



        logger.fine("will open indexWriter for '" + strIndexPathOrURL + "'");

        // wenn fäschlicherweise z.B. ein video-attachment als fulltext verarbeitet wird, haben wir riesige Docs, viel Speicher, lange Zeiten...aus
        // diesem Grund setzte ich die MaxFieldLength mal wieder auf limited
        @SuppressWarnings("deprecation")
        IndexWriter ourIndexWriter = new IndexWriter(dir, new IndexWriterConfig(getDefaultAnalyzer()).setOpenMode(OpenMode.CREATE));

        ourIndexWriter.close();

        return true;
    }





    // /**
    //  * Enable or disable native file locking. We recommend the native lock, which is also the default.
    //  *
    //  * @param bNativeFileLock true in the case you want to use native file OS locks. These could be problematic on NFS drives (see {@link NativeFSLockFactory}). I
    //  *            recommend to use the native File lock (stress tests on our NFS system have shown that this is really an atomar, working lock - the other lock leads to
    //  *            exceptions (at least in ealier versions of Lucene)
    //  */
    // static public void enableNativeFileLock(boolean bNativeFileLock)
    // {
    //     m_bNativeFileLock = bNativeFileLock;
    // }



    /**
     * Gets the default analyzer that will be used for writer creation
     * 
     * @return the default analyzer that will be used for writer creation
     */
    static public Analyzer getDefaultAnalyzer()
    {
        return m_analyzer4writer;
    }



    /**
     * Gets the default attribute name that will be used for RemotIndexReader creation
     * 
     * @return the default attribute name that will be used for RemotIndexReader creation
     */
    static public String getDefaultIndexIdAttribute()
    {
        return IndexAccessor.m_strIdAttributeName;
    }


    //
    // /**
    //  * Gets the reader for a given index path. The reader will be refreshed if there are any new changes in the index. In the case you pass an static reader ID to this
    //  * method, it will be identically to {@link #getIndexReader(String)}. You dont have to release a RemoteIndexReader.
    //  *
    //  * @param strIndexPathOrURL the path to the index where you want to read from
    //  *
    //  * @return the reader object that reflects the current state of the index
    //  *
    //  * @throws IOException
    //  * @throws CorruptIndexException
    //  * @throws URISyntaxException
    //  */
    // public synchronized static RemoteIndexReader getFreshIndexReader(String strIndexPathOrURL) throws CorruptIndexException, IOException, URISyntaxException
    // {
    //     refreshIndexReader(strIndexPathOrURL, false);
    //
    //     return getIndexReader(strIndexPathOrURL);
    // }

    //
    //
    // /**
    //  * Gets the reader for the given index path. The reader will be created when necessary. In the case the specified directory does not exists or is empty, an empty
    //  * index will NOT be created.<br>
    //  * Remark:<br>
    //  * Note that refreshing a reader is a relatively expensive operation. The reader Object returned from this method must not reflect the current state of the index. To
    //  * get a guaranteed up to date, refreshed reader object, you have the following possibilities:<br>
    //  * <li>invoke one of the methods {@link #refreshIndexReader(String)} or {@link #refreshAllIndexReaders()}</li> <li>use the method {@link #getFreshIndexReader(String)}
    //  * </li> <br>
    //  * You can also set a time intervall where all reader Objects will be refreshed for {@link #getLuceneIndexReader(String, boolean)} periodically with the method
    //  * {@link #setReaderRefreshIntervall(long)} <br>
    //  * You dont have to release a RemoteIndexReader.
    //  *
    //  * @param strIndexPathOrURL the path to the index you wants to read from
    //  *
    //  * @return the index reader object
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URISyntaxException
    //  */
    // public synchronized static RemoteIndexReader getIndexReader(String strIndexPathOrURL) throws CorruptIndexException, IOException, URISyntaxException
    // {
    //     return getIndexReader(strIndexPathOrURL, false);
    // }


    //
    // /**
    //  * Gets the reader for the given index path. The reader will be created when necessary. In the case the specified directory does not exists or is empty, an empty
    //  * index will be created, if you want.<br>
    //  * Remark:<br>
    //  * Note that refreshing a reader is a relatively expensive operation. The reader Object returned from this method must not reflect the current state of the index. To
    //  * get a guaranteed up to date, refreshed reader object, you have the following possibilities:<br>
    //  * <li>invoke one of the methods {@link #refreshIndexReader(String)} or {@link #refreshAllIndexReaders()}</li> <li>use the method {@link #getFreshIndexReader(String)}
    //  * </li> <br>
    //  * You can also set a time intervall where all reader Objects will be refreshed for {@link #getIndexReader(String, boolean)} periodically with the method
    //  * {@link #setReaderRefreshIntervall(long)} <br>
    //  * You dont have to release a RemoteIndexReader.
    //  *
    //  * @param strIndexPathOrURL the path to the index you wants to read from. This can be a simple path 'e.g. /home/hitzliputzli' or with URI Syntax
    //  *            ('file:\\/home/hitzliputzli'). In the case the specified protocoll is not of type 'file', and delight is in the classpath, the method tries to create a
    //  *            delight client object.
    //  * @param bCreateIndexIfNotExist if true, the index will be created in the case he did not exist
    //  *
    //  * @return the index reader object
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  * @throws URISyntaxException
    //  */
    // synchronized static public RemoteIndexReader getIndexReader(String strIndexPathOrURL, boolean bCreateIndexIfNotExist) throws CorruptIndexException, IOException,
    //         URISyntaxException
    // {
    //
    //     RemoteIndexReader remoteIndexReader;
    //
    //
    //     if(isLocalPath(strIndexPathOrURL))
    //     {
    //         // lokal - wir rufen einfach die entsprechene LuceneReader-Methode einmal auf, um das Objekt intern zu erstellen
    //         IndexReader luceneIndexReader = getLuceneIndexReader(strIndexPathOrURL, bCreateIndexIfNotExist);
    //         releaseLuceneIndexReader(luceneIndexReader);
    //
    //         // das zugrundeliegende Objekt wurde initialisiert, nun einfach den String/Pfad basierten 'wrapper'
    //         remoteIndexReader = new RemoteIndexReaderImpl(strIndexPathOrURL, m_strIdAttributeName);
    //     }
    //     else
    //     {
    //         // wir versuchen, eine Verbindung zu einem RemoteReader aufzubauen
    //         strIndexPathOrURL = strIndexPathOrURL.replaceAll("/$", "");
    //         String strHandlerName = strIndexPathOrURL.substring(strIndexPathOrURL.lastIndexOf('/') + 1) + "_reader";
    //         String strServiceUrl = strIndexPathOrURL.replaceAll("/[^/]+$", "");
    //
    //
    //         remoteIndexReader = delight.connectingTo(strServiceUrl).usingApi(strHandlerName, RemoteIndexReader.class);
    //     }
    //
    //
    //     return remoteIndexReader;
    // }





    /**
     * Gets all index paths that are currently inside the reader cache
     * 
     * @return all index paths that are currently inside the reader cache
     */
    public static Set<String> getIndexReaderPathsAndIDs()
    {
        return m_hsIndexPathOrId2CurrentIndexReader.keySet();
    }


    //
    // synchronized static public RemoteIndexSearcher getIndexSearcher(String strIndexPathOrURL) throws CorruptIndexException, IOException, URISyntaxException
    // {
    //     RemoteIndexSearcher searcher4Index;
    //
    //
    //     if(isLocalPath(strIndexPathOrURL))
    //     {
    //
    //         // lokal - wir rufen einfach die entsprechene LuceneReader-Methode einmal auf, um das Objekt intern zu erstellen
    //         IndexReader luceneIndexReader = getLuceneIndexReader(strIndexPathOrURL, false);
    //         releaseLuceneIndexReader(luceneIndexReader);
    //
    //         // das zugrundeliegende Objekt wurde initialisiert, nun einfach den String/Pfad basierten 'wrapper'
    //         searcher4Index = new RemoteIndexSearcherImpl(strIndexPathOrURL, m_strIdAttributeName);
    //     }
    //     else
    //     {
    //
    //         // es gibt zumindest keinen lokalen Index - dann könnte es noch eine remotegeschichte sein
    //
    //         searcher4Index = m_hsIndexPathOrURL2CurrentRemoteSearcher.get(strIndexPathOrURL);
    //         if(searcher4Index == null)
    //         {
    //
    //             logger.fine("will create new remote searcher for index '" + strIndexPathOrURL + "'");
    //
    //             strIndexPathOrURL = strIndexPathOrURL.replaceAll("/$", "");
    //             String strHandlerName = strIndexPathOrURL.substring(strIndexPathOrURL.lastIndexOf('/') + 1) + "_searcher";
    //             String strServiceUrl = strIndexPathOrURL.replaceAll("/[^/]+$", "");
    //
    //
    //             searcher4Index = delight.connectingTo(strServiceUrl).usingApi(strHandlerName, RemoteIndexSearcher.class);
    //
    //
    //             m_hsIndexPathOrURL2CurrentRemoteSearcher.put(strIndexPathOrURL, searcher4Index);
    //         }
    //     }
    //
    //
    //     return searcher4Index;
    // }
    //


    /**
     * Gets a writer instance for an index. DON'T !!!!! close your writer afterwards - use the >>>>> releaseIndexWriter(..) <<<<< method instead, and make SURE not to
     * forget this. The close will be done automatically, and you would permit any other threads to work with the index by doing this. The default analyzer will be used <br>
     * In the case the specified directory does not exists or is empty, an empty index will be created.<br>
     * Remark:<br>
     * You can change the timeout Lucene waits for getting write access by setting IndexWriter.WRITE_LOCK_TIMEOUT<br>
     * It is in almost any case I can imagine no good idea to have an IndexWriter member variable that refers on the reference from this method. This will block all other
     * processes that wants to get access to the index. You can make this in a short-living Object, but know exactly what yo do...
     * 
     * @param strIndexPathOrURL the path to the index
     * 
     * @return a writer instance for the given index. Autocommit will be FALSE.
     * 
     * @throws CorruptIndexException
     * @throws LockObtainFailedException
     * @throws IOException
     */
    synchronized static public IndexWriter getIndexWriter(String strIndexPathOrURL) throws CorruptIndexException, LockObtainFailedException, IOException
    {
        if(getDefaultAnalyzer() == null) logger.severe("default analyzer is not set - this will cause a Nullpointer Exception. Set it before creating an IndexWriter.");
        return getIndexWriter(strIndexPathOrURL, getDefaultAnalyzer());
    }



    /**
     * Gets a writer instance for an index. DON'T !!!!! close your writer afterwards - use the >>>>> releaseWriter4DefaultIndex() <<<<< method instead, and make SHURE not
     * to forget this. The close will be done automatically, and you would permit any other threads to work with the index by doing this<br>
     * In the case the specified directory does not exists or is empty, an empty index will be created.<br>
     * Remark:<br>
     * You can change the timeout Lucene waits for getting write access by setting IndexWriter.WRITE_LOCK_TIMEOUT<br>
     * It is in almost any case I can imagine no good idea to have an IndexWriter member variable that refers on the reference from this method. This will block all other
     * processes that wants to get access to the index. You can make this in a short-living Object, but know exactly what yo do...
     * 
     * @param strIndexPathOrURL the path to the index
     * @param analyzer the Lucene analyzer that should be used for this writer creation
     * 
     * @return a writer instance for the given index. Autocommit will be FALSE.
     * 
     * @throws CorruptIndexException
     * @throws LockObtainFailedException
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    synchronized static public IndexWriter getIndexWriter(String strIndexPathOrURL, Analyzer analyzer) throws CorruptIndexException, LockObtainFailedException,
            IOException
    {

        // Haben wir schon einen geöffneten Writer?
        IndexWriter ourIndexWriter = m_hsIndexPathOrURL2Writer.get(strIndexPathOrURL);


        // wenn nicht, machen wir doch einen neuen
        if(ourIndexWriter == null)
        {
            // wenn es ein leeres directory ist oder es nicht existiert, dann machen wir auch gleich einen neuen Index
            createNewIndex(strIndexPathOrURL, false);

            FSDirectory dir = createFSDirectory(new File(strIndexPathOrURL));

            logger.fine("will open indexWriter for '" + strIndexPathOrURL + "'");

            ourIndexWriter = new IndexWriter(dir, new IndexWriterConfig( analyzer).setOpenMode(OpenMode.APPEND));

            m_hsIndexPathOrURL2Writer.put(strIndexPathOrURL, ourIndexWriter);
        }

        // wir verwalten Tokens - diese müssen wieder mit releaseWriter freigegeben werden
        Integer iOld = m_hsIndexWriter2WriterRefCount.get(ourIndexWriter);
        if(iOld == null)
            m_hsIndexWriter2WriterRefCount.put(ourIndexWriter, 1);
        else
            m_hsIndexWriter2WriterRefCount.put(ourIndexWriter, ++iOld);

        if(logger.isLoggable(Level.FINEST)) logger.finest("get indexWriter for '" + strIndexPathOrURL + "'\n" + LoggingUtils.getCurrentStackTrace());

        return ourIndexWriter;
    }



    /**
     * Gets all index paths that are currently inside the writer cache
     * 
     * @return all index paths that are currently inside the writer cache
     */
    public static Set<String> getIndexWriterPaths()
    {
        return m_hsIndexPathOrURL2Writer.keySet();
    }



    /**
     * This is an expert method - the use of RemoteIndexReader is recommended. Gets the reader for the given index path. The reader will be created when necessary. In the
     * case the specified directory does not exists or is empty, an empty index will be created, if you want.<br>
     * Remark:<br>
     * Note that refreshing a reader is a relatively expensive operation. The reader Object returned from this method must not reflect the current state of the index. To
     * get a guaranteed up to date, refreshed reader object. You have the following possibilities:<br>
     * <li>invoke one of the methods {@link #refreshIndexReader(String)} or {@link #refreshAllIndexReaders()}</li> <li>
     * </li> <br>
     * You can also set a time intervall where all reader Objects will be refreshed for {@link #getLuceneIndexReader(String, boolean)} periodically with the method
     * {@link #setReaderRefreshIntervall(long)} <br>
     * Don't forget to release your reader Object with {@link #releaseLuceneIndexReader(IndexReader)}
     * 
     * @param strIndexPathOrURL the path to the index you wants to read from. This can be a simple path 'e.g. /home/hitzliputzli' or with URI Syntax
     *            ('file:\\/home/hitzliputzli').
     * @param bCreateIndexIfNotExist if true, the index will be created in the case he did not exist
     * 
     * @return the index reader object
     * 
     * @throws CorruptIndexException
     * @throws IOException
     * @throws URISyntaxException
     */
    synchronized static public IndexReader getLuceneIndexReader(String strIndexPathOrURL, boolean bCreateIndexIfNotExist) throws CorruptIndexException, IOException,
            URISyntaxException
    {
        IndexReader reader = m_hsIndexPathOrId2CurrentIndexReader.get(strIndexPathOrURL);

        // wenn wir noch keinen haben, dann erstellen wir uns einen
        if(reader == null)
        {

            logger.fine("will create new reader for index '" + strIndexPathOrURL + "'");


            File fIndex = null;
            // die super-URI-Implementierung nimmt echt alles an, was auch keine Uri ist, ohne eine syntaxException - insbesondere einen Pfad :(

            if(strIndexPathOrURL.startsWith("file:"))
                fIndex = new File(new URI(strIndexPathOrURL));
            else
                fIndex = new File(strIndexPathOrURL);



            // wenn es ein leeres directory ist oder es nicht existiert, dann machen wir auch gleich einen neuen Index
            if(bCreateIndexIfNotExist) createNewIndex(strIndexPathOrURL, false);

            Directory dir = createFSDirectory(fIndex);


            reader = DirectoryReader.open(dir);


            // hier steht immer der neueste drin - die alten werden in der release-methode wieder zu gemacht
            m_hsIndexPathOrId2CurrentIndexReader.put(strIndexPathOrURL, reader);
        }


        // das Token wird für diesen Index inkrementiert
        Integer iOld = m_hsIndexReader2ReaderRefCount.get(reader);
        if(iOld == null)
        {
            m_hsIndexReader2ReaderRefCount.put(reader, 1);
            m_hsIndexReader2IndexPath.put(reader, strIndexPathOrURL);
        }
        else
            m_hsIndexReader2ReaderRefCount.put(reader, ++iOld);


        if(logger.isLoggable(Level.FINEST)) logger.finest("get reader for index '" + strIndexPathOrURL + "'\n" + LoggingUtils.getCurrentStackTrace());

        return reader;
    }



    synchronized static public IndexSearcher getLuceneIndexSearcher(String strIndexPathOrURL) throws CorruptIndexException, IOException, URISyntaxException
    {
        logger.fine("will create new searcher for index '" + strIndexPathOrURL + "'");

        IndexSearcher searcher4Index = new IndexSearcher(getLuceneIndexReader(strIndexPathOrURL, false));



        return searcher4Index;
    }



    synchronized static public IndexSearcher getLuceneMultiSearcher(LinkedHashSet<String> sIndexPathsOrURLs) throws CorruptIndexException, IOException,
            URISyntaxException
    {
        logger.fine("will create new searcher for index '" + sIndexPathsOrURLs + "'");

        IndexSearcher searcher4Index = new IndexSearcher(getLuceneMultiReader(sIndexPathsOrURLs, false));



        return searcher4Index;
    }



    /**
     * Gets the lucene MultiReader for all given LOCAL reader paths (paths that point to the file system, not to a remote index). The readers will be created when
     * necessary. In the case a specified directory does not exist or is empty, an empty index will be created, if you want.<br>
     * Remark:<br>
     * Note that refreshing a reader is a relatively expensive operation. The reader Object returned from this method must not reflect the current state of the index. To
     * get a guaranteed up to date, refreshed reader object, you have the following possibilities:<br>
     * <li>invoke one of the methods {@link #refreshIndexReader(String)} or {@link #refreshAllIndexReaders()}</li> <li>
     * </li> <br>
     * You can also set a time intervall where all reader Objects will be refreshed for {@link #getLuceneIndexReader(String, boolean)} periodically with the method
     * {@link #setReaderRefreshIntervall(long)} <br>
     * You dont have to release a RemoteIndexReader.
     * 
     * @param sIndexPathsOrURLs the paths to the indices you want to read from. This can be a simple path 'e.g. /home/hitzliputzli' or with URI Syntax
     *            ('file:\\/home/hitzliputzli'). In the case the specified protocoll is not of type 'file',
     * @param bCreateIndexIfNotExist if true, the index will be created in the case he did not exist
     * 
     * @return the index reader object
     * 
     * @throws CorruptIndexException
     * @throws IOException
     * @throws URISyntaxException
     */
    synchronized static public MultiReader getLuceneMultiReader(LinkedHashSet<String> sIndexPathsOrURLs, boolean bCreateIndexIfNotExist) throws CorruptIndexException,
            IOException, URISyntaxException
    {


        LinkedList<IndexReader> lReaders = new LinkedList<>();
        for (String strIndexPathOrUrl : sIndexPathsOrURLs)
        {

            if(isLocalPath(strIndexPathOrUrl))
            {
                // lokal - wir rufen einfach die entsprechene LuceneReader-Methode einmal auf, um das Objekt intern zu erstellen
                IndexReader luceneIndexReader = getLuceneIndexReader(strIndexPathOrUrl, bCreateIndexIfNotExist);


                lReaders.add(luceneIndexReader);
            }
            else
            {
                // ignore
            }

        }


        BetterMultiReader multiReader = new BetterMultiReader(lReaders.toArray(new IndexReader[0]), false);


        return multiReader;
    }


    //
    // /**
    //  * Gets a MultiReader that wrapps all index readers for the given Set of index paths. You dont have to release a RemoteIndexReader.
    //  *
    //  * @param indexPathsOrIDs2CreateIfNotExist the set of indices that should be wrapped by the MultiReader. The last reader in the list will be stable with respect to
    //  *            write modifications during the livetime of this MultiReader, because the documents index number will stay stable in this index. For each index, you can
    //  *            specify whether she should be created or not in the case it not exists.
    //  *
    //  * @return a MultiReader the wrapps all index readers for the given Set of index paths. You dont have to release a RemoteIndexReader.
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  */
    // public synchronized static RemoteIndexReader getMultiIndexReader(LinkedHashMap<String, Boolean> indexPathsOrIDs2CreateIfNotExist) throws CorruptIndexException,
    //         IOException
    // {
    //
    //     // wir trennen die lokalen von den remote-URLs. Mit den lokalen machen wir EINEN LuceneMultiReader, und dann packen wir die remotes dazu
    //
    //     // Wir trennen in remote-und lokale Indizes
    //     LinkedList<String> lLocalIndices = new LinkedList<>();
    //     LinkedList<String> lRemoteIndices = new LinkedList<>();
    //
    //     for (Entry<String, Boolean> strIndexPathOrURL2CreateIfNotExist : indexPathsOrIDs2CreateIfNotExist.entrySet())
    //     {
    //
    //         String strIndexPathOrURL = strIndexPathOrURL2CreateIfNotExist.getKey();
    //         Boolean bCreateIfNotExist = strIndexPathOrURL2CreateIfNotExist.getValue();
    //
    //         if(isLocalPath(strIndexPathOrURL))
    //         {
    //             lLocalIndices.add(strIndexPathOrURL);
    //             if(bCreateIfNotExist) createNewIndex(strIndexPathOrURL, false);
    //         }
    //         else
    //         {
    //             lRemoteIndices.add(strIndexPathOrURL);
    //         }
    //     }
    //
    //
    //     LinkedList<de.dfki.inquisition.lucene.RemoteIndexReader> llReaderz = new LinkedList<de.dfki.inquisition.lucene.RemoteIndexReader>();
    //
    //     // der lokale MultiReader
    //     de.dfki.inquisition.lucene.RemoteIndexReader localReader = new RemoteIndexReaderImpl(lLocalIndices.toArray(new String[0]));
    //     localReader.setIdAttributename(m_strIdAttributeName);
    //     llReaderz.add(localReader);
    //
    //
    //     // die remote reader
    //     for (String strRemoteURL : lRemoteIndices)
    //     {
    //
    //         try
    //         {
    //             // index creation is of no sense when we have a remote reader anyway
    //             de.dfki.inquisition.lucene.RemoteIndexReader reader = getIndexReader(strRemoteURL, false);
    //             // check if this reader is available
    //             reader.numDocs();
    //
    //             llReaderz.add(reader);
    //         }
    //         catch (Exception e)
    //         {
    //             logger.log(Level.SEVERE, "Exception while creating a remote index reader. The index '" + strRemoteURL + "' will be ignored. ('" + e.getMessage() + "')");
    //             logger.log(Level.FINE, "Exception for index '" + strRemoteURL + "': ", e);
    //         }
    //     }
    //
    //
    //     // und daraus erzeugen wir uns jetzt nen MultiReader
    //     if(llReaderz.size() == 1) return llReaderz.get(0);
    //
    //     RemoteMultiIndexReader multiReader = new RemoteMultiIndexReader(llReaderz.toArray(new de.dfki.inquisition.lucene.RemoteIndexReader[0]));
    //
    //
    //     return multiReader;
    // }



    // /**
    //  * Gets a MultiReader that wrapps all index readers for the given Set of index paths. You dont have to release a RemoteIndexReader.
    //  *
    //  * @param indexPathsOrIDs the set of indices that should be wrapped by the MultiReader. The last reader in the list will be stable with respect to write modifications
    //  *            during the livetime of this MultiReader, because the documents index number will stay stable in this index. For each index, the index will NOT created
    //  *            in the case it does not exists
    //  *
    //  * @return a MultiReader the wrapps all index readers for the given Set of index paths. You dont have to release a RemoteIndexReader.
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  */
    // public synchronized static RemoteIndexReader getMultiIndexReader(LinkedHashSet<String> indexPathsOrIDs) throws CorruptIndexException, IOException
    // {
    //     return getMultiIndexReader(indexPathsOrIDs, false);
    // }

    //
    //
    // /**
    //  * Gets a MultiReader that wrapps all index readers for the given Set of index paths. You dont have to release a RemoteIndexReader.
    //  *
    //  * @param indexPathsOrIDs the set of indices that should be wrapped by the MultiReader. The last reader in the list will be stable with respect to write modifications
    //  *            during the livetime of this MultiReader, because the documents index number will stay stable in this index. For each index, the index will NOT created
    //  *            in the case it does not exists (beside the last one if you want it)
    //  * @param bCreateLastIndexInListIfNotExist if true, the last index in the list will be created in the case it does not exist
    //  *
    //  * @return a MultiReader the wrapps all index readers for the given Set of index paths. You dont have to release a RemoteIndexReader.
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  */
    // public synchronized static RemoteIndexReader getMultiIndexReader(LinkedHashSet<String> indexPathsOrIDs, boolean bCreateLastIndexInListIfNotExist)
    //         throws CorruptIndexException, IOException
    // {
    //     LinkedHashMap<String, Boolean> hsIndexPathsOrIDs2CreateIfNotExist = new LinkedHashMap<String, Boolean>();
    //
    //
    //     int i = 0;
    //     for (String strIndexPathOrURL : indexPathsOrIDs)
    //     {
    //         boolean bCreateIfNotExist = false;
    //         if(i == indexPathsOrIDs.size() - 1) bCreateIfNotExist = bCreateLastIndexInListIfNotExist;
    //
    //         hsIndexPathsOrIDs2CreateIfNotExist.put(strIndexPathOrURL, bCreateIfNotExist);
    //
    //         i++;
    //     }
    //
    //     return getMultiIndexReader(hsIndexPathsOrIDs2CreateIfNotExist);
    // }


    //
    // /**
    //  * Gets a MultiReader that wrapps all currently cached index readers. You dont have to release a RemoteIndexReader.
    //  *
    //  * @param strLastIndexInListPathOrID this will be the last reader in the list of reader offered to the MultiReader Constructor. In this index you can write and read
    //  *            in parallel, because the document numbers will not change during writing (until index optimization). In the case you don't write to any index, the order
    //  *            is irrelevant and you can set this paraeter simply null
    //  *
    //  * @return a MultiReader that wrapps all currently cached index readers. You dont have to release a RemoteIndexReader.
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  */
    // public synchronized static RemoteMultiIndexReader getMultiIndexReader(String strLastIndexInListPathOrID) throws CorruptIndexException, IOException
    // {
    //     return getMultiIndexReader(strLastIndexInListPathOrID, false);
    // }



    // /**
    //  * Gets a MultiReader that wrapps all currently cached index readers. Don't forget to release it with {@link #releaseLuceneIndexReader(IndexReader)}
    //  *
    //  * @param strLastIndexInListPathOrID this will be the last reader in the list of reader offered to the MultiReader Constructor. In this index you can write and read
    //  *            in parallel, because the document numbers will not change during writing (until index optimization). In the case you don't write to any index, the order
    //  *            is irrelevant and you can set this paraeter simply null
    //  * @param bCreateLastIndexInListIfNotExist if true, the last index in the list will be created in the case it does not exist
    //  *
    //  * @return a MultiReader that wrapps all currently cached index readers. You dont have to release a RemoteIndexReader.
    //  *
    //  * @throws CorruptIndexException
    //  * @throws IOException
    //  */
    // public synchronized static RemoteMultiIndexReader getMultiIndexReader(String strLastIndexInListPathOrID, boolean bCreateLastIndexInListIfNotExist)
    //         throws CorruptIndexException, IOException
    // {
    //     LinkedList<RemoteIndexReader> llReaderz = new LinkedList<de.dfki.inquisition.lucene.RemoteIndexReader>();
    //
    //
    //     // der reader, auf den auch schreibend zugegriffen werden kann, machen wir am Schluß rein - ich habe die Hoffnung,
    //     // daß sich dann nicht die docIDs verschieben, wenn gleichzeitig geschrieben und in diesem und in externen Indices
    //     // gesucht wird...die externen müssen halt readonly sein...und des funzt auch :)
    //
    //
    //     HashSet<String> hsIndexPaths = new HashSet<String>();
    //     hsIndexPaths.addAll(getIndexReaderPathsAndIDs());
    //
    //     // aaalso. wir erstellen alle Readers, und für den letzten wird das Flag eingesetzt...
    //     for (String strIndexPathOrURL : hsIndexPaths)
    //     {
    //
    //         boolean bIsLast = strIndexPathOrURL.equals(strLastIndexInListPathOrID);
    //
    //         try
    //         {
    //
    //             de.dfki.inquisition.lucene.RemoteIndexReader reader;
    //             if(bIsLast)
    //                 reader = getIndexReader(strIndexPathOrURL, bCreateLastIndexInListIfNotExist);
    //             else
    //                 reader = getIndexReader(strIndexPathOrURL, false);
    //
    //
    //             if(strLastIndexInListPathOrID == null || llReaderz.size() == 0 || bIsLast)
    //                 llReaderz.addLast(reader);
    //             else
    //                 llReaderz.addFirst(reader);
    //
    //         }
    //         catch (Exception e)
    //         {
    //             logger.log(Level.SEVERE, "Exception while creating a MultiReader. The index '" + strIndexPathOrURL + "' will be ignored. ('" + e.getMessage() + "')");
    //             logger.log(Level.FINE, "Exception for index '" + strIndexPathOrURL + "': ", e);
    //         }
    //     }
    //
    //
    //     // und daraus erzeugen wir uns jetzt nen MultiReader
    //     RemoteMultiIndexReader multiReader = new RemoteMultiIndexReader(llReaderz.toArray(new RemoteIndexReader[0]));
    //
    //
    //     return multiReader;
    // }



    // synchronized static public RemoteIndexSearcher getMultiIndexSearcher(LinkedHashSet<String> indexPathsOrURLs) throws IOException, URISyntaxException
    // {
    //
    //     // - wir erzeugen uns einen searcher aus jeder Quelle - ganz einfach mit getIndexSearcher. Da wird dann auch die Unterscheidung zwischen
    //     // lokal- und remoteSearcher gemacht.
    //     // - wir nehmen den wunderschönen ParallelMultiSearcher - verteilte document frequency + multithreaded Suche....sehr schön :)...den gibts nicht mehr :(
    //
    //
    //
    //     // Wir trennen in remote-und lokale Indizes
    //     LinkedList<String> lLocalIndices = new LinkedList<>();
    //     LinkedList<String> lRemoteIndices = new LinkedList<>();
    //
    //     for (String strIndexPathOrURL : indexPathsOrURLs)
    //     {
    //         if(isLocalPath(strIndexPathOrURL))
    //         {
    //             lLocalIndices.add(strIndexPathOrURL);
    //         }
    //         else
    //         {
    //             lRemoteIndices.add(strIndexPathOrURL);
    //         }
    //     }
    //
    //
    //     LinkedList<RemoteIndexSearcher> llSearcherz = new LinkedList<RemoteIndexSearcher>();
    //
    //     // der lokale MultiSearcher
    //     RemoteIndexSearcherImpl localSearcher = new RemoteIndexSearcherImpl(lLocalIndices.toArray(new String[0]));
    //     localSearcher.setIdAttributename(m_strIdAttributeName);
    //     llSearcherz.add(localSearcher);
    //
    //
    //     // die remote reader
    //     for (String strRemoteURL : lRemoteIndices)
    //     {
    //
    //         try
    //         {
    //             RemoteIndexSearcher searcher = getIndexSearcher(strRemoteURL);
    //
    //             // check if the remote index is up and running
    //             searcher.maxDoc();
    //
    //             llSearcherz.add(searcher);
    //         }
    //         catch (Exception e)
    //         {
    //             logger.log(Level.SEVERE, "Exception while creating a MultiSearcher. The index '" + strRemoteURL + "' will be ignored. ('" + e.getMessage() + "')");
    //             logger.log(Level.FINE, "Exception for index '" + strRemoteURL + "': ", e);
    //         }
    //     }
    //
    //
    //     // und daraus erzeugen wir uns jetzt nen MultiSearcer
    //     if(llSearcherz.size() == 1) return llSearcherz.get(0);
    //
    //     RemoteMultiIndexSearcher multiSearcher = new RemoteMultiIndexSearcher(llSearcherz.toArray(new RemoteIndexSearcher[0]));
    //
    //
    //     return multiSearcher;
    //
    //
    //
    //     //
    //     //
    //     //
    //     //
    //     // LinkedList<RemoteIndexSearcher> llSearchables = new LinkedList<RemoteIndexSearcher>();
    //     //
    //     // for (String strIndexPathOrURL : indexPathsOrURLs)
    //     // {
    //     // try
    //     // {
    //     //
    //     // RemoteIndexSearcher searcher = getIndexSearcher(strIndexPathOrURL);
    //     // llSearchables.add(searcher);
    //     //
    //     // }
    //     // catch (Exception e)
    //     // {
    //     // logger.log(Level.SEVERE, "Exception while creating a MultiSearcher. The index '" + strIndexPathOrURL + "' will be ignored. ('" + e.getMessage() + "')");
    //     // logger.log(Level.FINE, "Exception for index '" + strIndexPathOrURL + "': ", e);
    //     // }
    //     // }
    //     //
    //     //
    //     // RemoteMultiIndexSearcher searcher = new RemoteMultiIndexSearcher(llSearchables.toArray(new RemoteIndexSearcher[0]));
    //     //
    //     //
    //     // return searcher;
    // }


    //
    // synchronized static public RemoteIndexSearcher getMultiIndexSearcher(String strLastIndexInListPathOrID) throws IOException, URISyntaxException
    // {
    //
    //     LinkedList<String> llIndices = new LinkedList<String>();
    //
    //
    //     // der reader, auf den auch schreibend zugegriffen werden kann, machen wir am Schluß rein - ich habe die Hoffnung,
    //     // daß sich dann nicht die docIDs verschieben, wenn gleichzeitig geschrieben und in diesem und in externen Indices
    //     // gesucht wird...die externen müssen halt readonly sein...und des funzt auch :)
    //
    //
    //     HashSet<String> hsIndexPaths = new HashSet<String>();
    //     hsIndexPaths.addAll(getIndexReaderPathsAndIDs());
    //
    //     // aaalso. wir erstellen alle Readers, und für den letzten wird das Flag eingesetzt...
    //     for (String strIndexPathOrURL : hsIndexPaths)
    //     {
    //
    //         boolean bIsLast = strIndexPathOrURL.equals(strLastIndexInListPathOrID);
    //
    //         if(strLastIndexInListPathOrID == null || llIndices.size() == 0 || bIsLast)
    //             llIndices.addLast(strIndexPathOrURL);
    //         else
    //             llIndices.addFirst(strIndexPathOrURL);
    //     }
    //
    //
    //     return getMultiIndexSearcher(new LinkedHashSet<String>(llIndices));
    // }





    /**
     * Gets the time intervall all reader objects will be refreshed automatically. After a refresh, all Objects from subsequent calls of {@link #getLuceneIndexReader(String, boolean)}
     * will reflect the current state of an index, with any changes done.
     * 
     * @return the reader refresh time intervall
     */
    static public long getReaderRefreshIntervall()
    {
        return m_lReaderRefreshIntervall;
    }



    // /**
    //  * Gets whether native file locking is enabled or not
    //  *
    //  * @return whether native file locking is enabled or not
    //  */
    // static public boolean isNativeFileLockEnabled()
    // {
    //     return m_bNativeFileLock;
    // }




    /**
     * Returns true in the case a reader object for a given index path is inside the cache
     * 
     * @param strIndexPathOrURL the index path for the reader object
     * 
     * @return true in the case a reader object for the given index path is inside the cache
     */
    static public boolean isReaderInCache(String strIndexPathOrURL)
    {
        return m_hsIndexPathOrId2CurrentIndexReader.containsKey(strIndexPathOrURL);
    }



    /**
     * Refreshs all index readers
     * 
     * @throws CorruptIndexException
     * @throws IOException
     * @throws URISyntaxException
     */
    synchronized static public void refreshAllIndexReaders() throws CorruptIndexException, IOException, URISyntaxException
    {
        LinkedList<String> llKeys = new LinkedList<String>();
        llKeys.addAll(m_hsIndexPathOrId2CurrentIndexReader.keySet());

        for (String strIndexPathOrURL : llKeys)
            refreshIndexReader(strIndexPathOrURL);

    }







    /**
     * Refreshs an index reader for a given path. In the case the indexReader was not formerly created by {@link #getLuceneIndexReader(String, boolean)}, it will be
     * created. In the case you will pass the ID of a static Reader, the method will do nothing.
     * 
     * @param strIndexPath the path to the lucene index
     * 
     * @throws CorruptIndexException
     * @throws IOException
     * @throws URISyntaxException
     */
    synchronized static public void refreshIndexReader(String strIndexPath) throws CorruptIndexException, IOException, URISyntaxException
    {
        refreshIndexReader(strIndexPath, false);
    }



    // static public boolean isLocalPath(String strIndexPathOrURL)
    // {
    // try
    // {
    //
    // if(strIndexPathOrURL == null) return false;
    //
    // File fIndex = null;
    // // die super-URI-Implementierung nimmt echt alles an, was auch keine Uri ist, ohne eine syntaxException - insbesondere einen Pfad :(
    //
    // if(strIndexPathOrURL.startsWith("file:"))
    //
    // fIndex = new File(new URI(strIndexPathOrURL));
    // else
    // fIndex = new File(strIndexPathOrURL);
    //
    //
    // if(fIndex.exists()) return true;
    //
    // return false;
    //
    //
    // }
    // catch (URISyntaxException e)
    // {
    // return false;
    // }
    //
    // }



    /**
     * Refreshs an index reader for a given path. In the case the indexReader was not formerly created by {@link #getLuceneIndexReader(String, boolean)}, it will be
     * created. In the case the index does not exist, it will be created, if you want. In the case you will pass the ID of a static Reader, the method will do nothing.
     * 
     * @param strIndexPath the path to the lucene index
     * @param bCreateIndexIfNotExist if true, the index will be created in the case he did not exist
     * 
     * @throws CorruptIndexException
     * @throws IOException
     * @throws URISyntaxException
     */
    synchronized static public void refreshIndexReader(String strIndexPath, boolean bCreateIndexIfNotExist) throws CorruptIndexException, IOException, URISyntaxException
    {

        // haben wir schon einen?
        IndexReader readerOld = getLuceneIndexReader(strIndexPath, bCreateIndexIfNotExist);

        // wenn es ein statischer Reader ist, dann wird der ned refreshed
        if(m_hsStaticIndexReaderSet.contains(readerOld)) return;
        // wenn es kein DirectoryReader ist, können wir ihn nicht refreshen
        if(!(readerOld instanceof DirectoryReader)) return;
        DirectoryReader dirReader = (DirectoryReader) readerOld;

        try
        {
            if(dirReader.isCurrent()) return;

            logger.info("will refresh reader for index '" + strIndexPath + "'");

            // den neuen erstellen
            // Directory dir = createFSDirectory(new File(strIndexPath));
            //
            // if(m_bLoadReadersInMemory) dir = new RAMDirectory(dir);
            //
            // IndexReader readerNew = IndexReader.open(dir, true);
            IndexReader readerNew = DirectoryReader.openIfChanged(dirReader);


            // hier steht immer der neueste drin - die alten werden in der release-methode wieder zu gemacht
            m_hsIndexPathOrId2CurrentIndexReader.put(strIndexPath, readerNew);

        }
        catch (org.apache.lucene.store.AlreadyClosedException e)
        {
            logger.warning("reader for '" + strIndexPath + "' was closed at refresh time");
        }
        finally
        {
            // der alte Reader wird dann geschlossen, wenn er nicht mehr gebraucht wird
            releaseLuceneIndexReader(readerOld);
        }

    }





    /**
     * Release your indexWriter that you get with getIndexWriter - in any case. In the case the IndexWriter is no more needed by some Instance, it will be commited and
     * closed.
     * 
     * @param indexWriter the writer Object that should be released
     */
    synchronized static public void releaseIndexWriter(IndexWriter indexWriter)
    {
        try
        {
            // wir dekrementieren den count für den aktuellen Index
            Integer iOld = m_hsIndexWriter2WriterRefCount.get(indexWriter);
            if(iOld == null || iOld == 0)
            {
                logger.warning("have no writer index token for '" + indexWriter + "'");
                return;
            }

            // das müssen wir an dieser Stelle machen - wenn der writer geclosed ist, dann wirft getDirectory eine Exception
            if(!(indexWriter.getDirectory() instanceof FSDirectory)) throw new IllegalStateException("Directory is not of type FSDirectory");

            String strIndexPathOrURL = ((FSDirectory) indexWriter.getDirectory()).getDirectory().toAbsolutePath().toString();


            int iNew = --iOld;

            String strDontCloseIndexWriters = System.getProperty("de.dfki.inquisition.lucene.IndexAccessor.DontCloseIndexWriters");
            boolean bIgnoreClose = false;
            if(strDontCloseIndexWriters != null) bIgnoreClose = Boolean.parseBoolean(strDontCloseIndexWriters);

            if(iNew == 0 && !bIgnoreClose)
            {
                // wenn wir bei 0 sind, dann mache mer des Ding gleich zu
                Set<Entry<String, IndexWriter>> entrySet = m_hsIndexPathOrURL2Writer.entrySet();
                Iterator<Entry<String, IndexWriter>> itEntries = entrySet.iterator();
                while (itEntries.hasNext())
                {
                    Entry<String, IndexWriter> entry = itEntries.next();
                    if(entry.getValue().equals(indexWriter)) itEntries.remove();
                }


                m_hsIndexWriter2WriterRefCount.remove(indexWriter);


                logger.fine("will close indexWriter for '" + strIndexPathOrURL + "'");

                indexWriter.commit();
                if(isLocalPath(strIndexPathOrURL)) indexWriter.close();
            }
            else
                m_hsIndexWriter2WriterRefCount.put(indexWriter, iNew);

            if(logger.isLoggable(Level.FINEST))
            {
                if(bIgnoreClose)
                    logger.finest("indexWriter '" + strIndexPathOrURL + "' released - closing IGNORED (writer is still open)\n" + LoggingUtils.getCurrentStackTrace());
                else
                    logger.finest("indexWriter '" + strIndexPathOrURL + "' released\n" + LoggingUtils.getCurrentStackTrace());
            }

        } catch (IOException e)
        {
            logger.severe(ExceptionUtils.createStackTraceString(e));
        }
    }



    /**
     * This is an expert method - the use of RemoteIndexReader is recommended (You don't need to release it). Releases your reader Object in the case you don't need it
     * anymore. In the case every instance has released a specific index path, the reader object will be closed.
     * 
     * @param reader the IndexReader Object you gets formerly with IndexAccessor
     */
    synchronized static public void releaseLuceneIndexReader(IndexReader reader)
    {

        try
        {

            if(reader instanceof BetterMultiReader)
            {
                for (IndexReader subReader : ((BetterMultiReader) reader).getSubReaders())
                    releaseLuceneIndexReader(subReader);

                return;
            }


            String strIndexPathOrURL4Reader = m_hsIndexReader2IndexPath.get(reader);
            if(strIndexPathOrURL4Reader == null)
                logger.severe("have no path entry for reader. This is a hint to an error, e.g. you have released the reader too often, or the reader was not created with IndexAccessor.");


            Integer iOldRefCount = m_hsIndexReader2ReaderRefCount.get(reader);

            if(iOldRefCount == null || iOldRefCount == 0)
            {
                logger.warning("have no reader index token for '" + strIndexPathOrURL4Reader + "'");
                return;
            }

            int iNew = --iOldRefCount;

            if(iNew == 0)
            {
                // wenn wir bei 0 sind, dann mache mer des Ding gleich zu - wenn es nicht noch im Cache bleiben soll
                m_hsIndexReader2ReaderRefCount.remove(reader);
                m_hsIndexReader2IndexPath.remove(reader);

                // wir schliessen den nur, wenn es nicht der aktuelle aus der hashmap ist - ansonsten müssten wir ihn ständig wieder neu erzeugen.
                // der aktuelle wir dann geschlossen, wenn es einen neueren gibt oder explizit mit removeReaderFromCache

                // wenn vorher gesagt wurde (mit removeReaderFromCacheWhenPossible), daß des Teil geschlossen werden soll, machen wir es auch zu

                if(!m_hsIndexPathOrId2CurrentIndexReader.containsValue(reader))
                {
                    // es ist nicht der aktuelle reader
                    if(isLocalPath(strIndexPathOrURL4Reader))
                    {
                        logger.info("will close indexReader '" + strIndexPathOrURL4Reader + "'");
                        reader.close();
                    }

                }
                else if(m_hsReader2Remove.contains(reader)) removeReaderFromCache(strIndexPathOrURL4Reader);

            }
            else
                m_hsIndexReader2ReaderRefCount.put(reader, iNew);


            if(logger.isLoggable(Level.FINEST)) logger.finest("indexReader '" + strIndexPathOrURL4Reader + "' released\n" + LoggingUtils.getCurrentStackTrace());


        }
        catch (IOException e)
        {
            logger.severe(ExceptionUtils.createStackTraceString(e));
        }
    }



    synchronized static public void releaseLuceneIndexSearcher(IndexSearcher searcher)
    {
        releaseLuceneIndexReader(searcher.getIndexReader());
    }



    /**
     * Removes an closes the reader object for a given index path from the cache. This is only possible in the case this object is no more in use - the method will throw
     * an exception otherwise.
     * 
     * @param strIndexPathOrURL the path to the index
     * 
     * @throws IOException
     */
    synchronized static public void removeReaderFromCache(String strIndexPathOrURL) throws IOException
    {
        // wir haben immer den aktuellen reader für einen index im Speicher - hier können wir ihn wieder entfernen, um den Speicher freizugeben

        // wenn der alte Reader nicht mehr benötigt wird, dann wird er geschlossen
        IndexReader reader = m_hsIndexPathOrId2CurrentIndexReader.get(strIndexPathOrURL);

        if(m_hsIndexReader2ReaderRefCount.get(reader) == null)
        {
            logger.fine("will close indexReader '" + strIndexPathOrURL + "'");
            m_hsIndexPathOrId2CurrentIndexReader.remove(strIndexPathOrURL);
            m_hsStaticIndexReaderSet.remove(reader);

            if(isLocalPath(m_hsIndexReader2IndexPath.get(reader))) reader.close();

            m_hsReader2Remove.remove(reader);
        }
        else
        {
            throw new IllegalStateException("Cannot remove reader object for '" + strIndexPathOrURL
                    + "' from cache. It is still in use. Did you forget an releaseIndexReader(..) invocation?");

        }
    }



    /**
     * Removes an closes the reader object for a given index path from the cache. This is only possible in the case this object is no more in use - otherwise, the reader
     * Object will be removed from the cache immediately when it is no more in use.
     * 
     * @param strIndexPathOrURL the path to the index
     * 
     * @return READER_CLOSED in the case the reader was closed immediately, READER_IN_QUEUE if it is in the queue of 'to close readers' now. If the reader is not inside
     *         the cache, the method will return READER_NOT_IN_CACHE
     * 
     * @throws IOException
     */
    synchronized static public ReaderStatus removeReaderFromCacheWhenPossible(String strIndexPathOrURL) throws IOException
    {
        // wir haben immer den aktuellen reader für einen index im Speicher - hier können wir ihn wieder entfernen, um den Speicher freizugeben

        if(!isReaderInCache(strIndexPathOrURL)) return ReaderStatus.READER_NOT_IN_CACHE;

        // wenn der alte Reader nicht mehr benötigt wird, dann wird er geschlossen
        IndexReader reader = m_hsIndexPathOrId2CurrentIndexReader.get(strIndexPathOrURL);

        if(m_hsIndexReader2ReaderRefCount.get(reader) == null)
        {
            logger.fine("will close indexReader '" + strIndexPathOrURL + "'");
            m_hsIndexPathOrId2CurrentIndexReader.remove(strIndexPathOrURL);
            m_hsStaticIndexReaderSet.remove(reader);

            if(isLocalPath(m_hsIndexReader2IndexPath.get(reader))) reader.close();

            return ReaderStatus.READER_CLOSED;

        }
        else
        {
            m_hsReader2Remove.add(reader);

            return ReaderStatus.READER_IN_QUEUE;
        }
    }




    // /**
    //  * Simply removes a formerly cached Searcher Object from the cache. Only remote Searcher proxies are cached - so this is only to give a possibility to free the memory
    //  * again (nevertheless, there should be not much amount of memory consumtion - in the case you have not thousands of searcher objects, you should be able to ignore
    //  * this...(hehe - I didn't say that ;) )
    //  *
    //  * @param strIndexPathOrURL the index for which you want to remove the according searcher proxy object out of the internal cache
    //  */
    // synchronized static public void removeSearcherFromCache(String strIndexPathOrURL)
    // {
    //     m_hsIndexPathOrURL2CurrentRemoteSearcher.remove(strIndexPathOrURL);
    // }



    /**
     * Removes and closes all cached reader objects that are not in use. This method can be used safely at any time, the only disadvantage is that an subsequent
     * invocation of {@link #getLuceneIndexReader(String, boolean)} for one of these indices will take longer time.
     * 
     * @throws IOException
     */
    static public void removeUnusedReadersFromCache() throws IOException
    {
        LinkedList<String> llIndexPaths = new LinkedList<String>();

        llIndexPaths.addAll(m_hsIndexPathOrId2CurrentIndexReader.keySet());

        for (String strIndexPathOrURL : llIndexPaths)
            try
            {
                removeReaderFromCache(strIndexPathOrURL);
            }
            catch (IllegalStateException e)
            {
                if(!e.getMessage().startsWith("Cannot remove reader object for")) throw e;
            }
    }



    /**
     * Sets the default analyzer that will be used for writer creation
     * 
     * @param analyzer the default analyzer that will be used for writer creation
     */
    static public void setDefaultAnalyzer(Analyzer analyzer)
    {
        m_analyzer4writer = analyzer;
    }



    /**
     * Sets the default attribute name that will be used for RemotIndexReader creation
     * 
     * @param strIdAttributeName the default attribute name that will be used for RemotIndexReader creation
     */
    static public void setDefaultIndexIdAttribute(String strIdAttributeName)
    {
        IndexAccessor.m_strIdAttributeName = strIdAttributeName;
    }



    /**
     * Sets the time intervall all reader objects will be refreshed automatically. After a refresh, all Objects from subsequent calls of {@link #getLuceneIndexReader(String, boolean)}
     * will reflect the current state of an index, with any changes done.
     * 
     * @param lMillis the time intervall the reader should be refreshed
     * 
     * @return the former time intervall
     */
    static public long setReaderRefreshIntervall(long lMillis)
    {
        long lOld = m_lReaderRefreshIntervall;

        m_lReaderRefreshIntervall = lMillis;

        return lOld;
    }



    protected static FSDirectory createFSDirectory(File fDirPath) throws IOException
    {
        // das muß man so umständlich mit setLockfactory machen - wenn man einfach initial das die erstellt, und das dir wurde mit einer anderen
        // LockFactory erstellt, dann kommt ne Exception


        // null heißt SimpleFileLock (ich hab gekuckt ;) )
        FSDirectory dir = FSDirectory.open(fDirPath.toPath());

        // NativeFSLockFactory lockFactory = new NativeFSLockFactory(fDirPath);
        // lockFactory.setLockPrefix("indexAccessor");
        // if(isNativeFileLockEnabled()) dir.setLockFactory(lockFactory);

        return dir;
    }




    /**
     * Closes all reader and writer objects. This is mainly for the shutdown hook, to make shure that no other processes will be blocked by non-closed Objects
     * 
     * @throws IOException
     */
    protected static void forceCloseAll() throws IOException
    {
        if(m_hsIndexReader2ReaderRefCount.size() == 0 && m_hsIndexPathOrURL2Writer.size() == 0) return;

        logger.info("closing of all index readers and writers will be forced " + m_hsIndexReader2ReaderRefCount.size() + " reader(s), "
                + m_hsIndexPathOrURL2Writer.size() + " writer(s)");


        for (IndexReader reader : m_hsIndexReader2ReaderRefCount.keySet())
            if(isLocalPath(m_hsIndexReader2IndexPath.get(reader))) reader.close();

        for (Entry<String, IndexWriter> pathOrURL2Writer : m_hsIndexPathOrURL2Writer.entrySet())
        {

            String strPath = pathOrURL2Writer.getKey();
            IndexWriter writer = pathOrURL2Writer.getValue();
            writer.commit();

            if(isLocalPath(strPath)) writer.close();
        }
    }



    /**
     * Gets all reader Objects that should be removed from the cache immediately when they are no more in use
     * 
     * @return all reader Objects that should be removed from the cache immediately when they are no more in use
     */
    protected static HashSet<IndexReader> getReader2RemoveQueue()
    {
        return m_hsReader2Remove;
    }






    /**
     * Checks whether the given URL is a local one or not. Local means that the URL starts with 'file:' or that this path exists on the local storage.
     */
    protected static boolean isLocalPath(String strIndexPathOrURL)
    {
        if(StringUtils.nullOrWhitespace(strIndexPathOrURL)) return false;

        File fIndex = null;
        // die super-URI-Implementierung nimmt echt alles an, was auch keine Uri ist, ohne eine syntaxException - insbesondere einen Pfad :(

        if(strIndexPathOrURL.startsWith("file:")) return true;

        fIndex = new File(strIndexPathOrURL);


        if(fIndex.exists()) return true;


        return false;
    }


}
