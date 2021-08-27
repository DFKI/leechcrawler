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

package de.dfki.km.leech.parser;



import de.dfki.inquisitor.collections.MultiValueHashMap;
import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.config.DirectoryCrawlerContext;
import de.dfki.km.leech.detect.DatasourceMediaTypes;
import de.dfki.km.leech.io.URLStreamProvider;
import de.dfki.km.leech.util.OSUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.slf4j.LoggerFactory;
import org.xml.sax.ContentHandler;

import javax.mail.URLName;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.SynchronousQueue;



/**
 * A CrawlerParser implementation that can crawl file system directories. Configure it by specifying a {@link CrawlerContext} and a
 * {@link DirectoryCrawlerContext} object inside the {@link ParseContext} object for the crawl.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class DirectoryCrawlerParser extends CrawlerParser
{

    protected static class OneAfterOneIterator implements Iterator<MultiValueHashMap<String, Object>>
    {

        static final MultiValueHashMap<String, Object> m_noMoreLeftMarker = new MultiValueHashMap<String, Object>();

        protected MultiValueHashMap<String, Object> m_nextElement;

        protected SynchronousQueue<MultiValueHashMap<String, Object>> m_synchronousQueue = new SynchronousQueue<MultiValueHashMap<String, Object>>();



        public void addNextElement(MultiValueHashMap<String, Object> nextElement)
        {
            try
            {

                m_synchronousQueue.put(nextElement);

            }
            catch (InterruptedException e)
            {
                LoggerFactory.getLogger(DirectoryCrawlerParser.OneAfterOneIterator.class.getName()).error("Error", e);
            }

        }



        @Override
        public boolean hasNext()
        {

            try
            {

                m_nextElement = m_synchronousQueue.take();

                if(m_noMoreLeftMarker == m_nextElement) return false;

                return true;

            }
            catch (InterruptedException e)
            {
                LoggerFactory.getLogger(DirectoryCrawlerParser.OneAfterOneIterator.class.getName()).error("Error", e);
            }


            return false;
        }



        @Override
        public MultiValueHashMap<String, Object> next()
        {
            return m_nextElement;
        }



        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

    }





    private static final long serialVersionUID = 1824851369780822093L;



    protected Leech m_leech;




    /**
     * Checks whether this file is inside the configured constraints (hidden files, symbolic links, etc) or not
     * 
     * @param fFile2Check the file to check whether it is in the configured constraints
     * @param crawlerContext the context object with the general constraints
     * @param directoryCrawlerContext the context Object with the directory related constraints
     * 
     * @return null in the case the file is outside the constraints, a canonical file object of the given file otherwise
     */
    protected File checkIfInConstraints(File fFile2Check, CrawlerContext crawlerContext, DirectoryCrawlerContext directoryCrawlerContext)
    {

        File finalFile = fFile2Check;

        try
        {
            // determine absolute and canonical paths
            String strAbsolutePath = fFile2Check.getAbsolutePath();
            String strCanonicalPath = fFile2Check.getCanonicalPath();

            // optionally skip symbolic links
            if(!directoryCrawlerContext.getFollowSymbolicLinks() && !strAbsolutePath.equals(strCanonicalPath))
            {
                if(crawlerContext.getVerbose())
                    LoggerFactory.getLogger(DirectoryCrawlerParser.class.getName()).info(
                            "File " + fFile2Check.toURI() + " is a symbolic link that should be ignored. Skipping.");
                return null;
            }

            // create the canonical File
            finalFile = new File(strCanonicalPath);

        }
        catch (IOException e)
        {
            LoggerFactory.getLogger(DirectoryCrawlerParser.class.getName()).warn(
                    "Unable to resolve file to its canonical form, continuing with original file: " + fFile2Check, e);
        }


        if(!crawlerContext.getURLFilter().accept(finalFile.toURI().toString()))
        {
            if(crawlerContext.getVerbose())
                LoggerFactory.getLogger(CrawlerParser.class.getName()).info(
                        "File " + finalFile.toURI() + " is outside the URL constraints for this data source. Skipping.");
            return null;
        }


        if(directoryCrawlerContext.getIgnoreHiddenFiles() && finalFile.isHidden())
        {
            // in the case this file is hidden, we also ignore it silently
            return null;
        }


        // Dont crawl into MacOSX bundles.
        if(OSUtils.isMacOSXBundle(finalFile)) return null;


        if(!finalFile.canRead())
        {
            if(crawlerContext.getVerbose())
                LoggerFactory.getLogger(DirectoryCrawlerParser.class.getName()).info("Can't read file " + finalFile.toURI() + ". Skipping.");
            return null;
        }




        return finalFile;
    }







    @Override
    protected Iterator<MultiValueHashMap<String, Object>> getSubDataEntitiesInformation(InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context) throws Exception
    {

        // was crawlen wir hier eigentlich?
        String strDirName = metadata.get(Metadata.SOURCE);
        // und wie?
        final CrawlerContext crawlerContext = context.get(CrawlerContext.class, new CrawlerContext());
        final DirectoryCrawlerContext directoryCrawlerContext = context.get(DirectoryCrawlerContext.class, new DirectoryCrawlerContext());




        final File fDir = new File(new URL(strDirName).toURI());

        if(!fDir.isDirectory()) throw new IllegalStateException("' " + strDirName + "' is no directory");


        final File fFinalDir = checkIfInConstraints(fDir, crawlerContext, directoryCrawlerContext);

        if(fFinalDir == null) return new LinkedList<MultiValueHashMap<String, Object>>().iterator();



        final OneAfterOneIterator oneAfterOneIterator = new OneAfterOneIterator();

        // wir übernehmen das Konzept aus Aperture mit dem FileFilter. Des isch ned schlecht.Spart memory.
        Thread listFilesThread = new Thread(new Runnable()
        {


            boolean m_bStopWasRequested = false;



            @Override
            public void run()
            {

                fFinalDir.listFiles(new FileFilter()
                {

                    @Override
                    public boolean accept(File fSubFile2Check)
                    {
                        // we always return false - this saves memory

                        if(crawlerContext.stopRequested())
                        {
                            if(!m_bStopWasRequested) m_bStopWasRequested = true;
                            return false;
                        }


                        File fCheckedFile = checkIfInConstraints(fSubFile2Check, crawlerContext, directoryCrawlerContext);

                        if(fCheckedFile == null) return false;

                        MultiValueHashMap<String, Object> hsEntityInformation = new MultiValueHashMap<String, Object>();

                        hsEntityInformation.add("fileObject", fCheckedFile);
                        hsEntityInformation.add(CrawlerParser.SOURCEID, fCheckedFile.getAbsolutePath());

                        oneAfterOneIterator.addNextElement(hsEntityInformation);

                        return false;
                    }
                });

                // wir markieren das Ende mit einer eigens dafür erstellten Konstante
                if(!m_bStopWasRequested && !crawlerContext.stopRequested())
                    oneAfterOneIterator.addNextElement(OneAfterOneIterator.m_noMoreLeftMarker);

            }
        }, "DirectoryCrawlserParser listFiles");

        listFilesThread.start();



        return oneAfterOneIterator;
    }



    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context)
    {
        return Collections.singleton(DatasourceMediaTypes.DIRECTORY);
    }






    @Override
    protected void processCurrentDataEntity(InputStream stream, Metadata metadata, ContentHandler handler2use4recursiveCall, ParseContext context)
            throws Exception
    {
        // NOP - wie don't process directories - we only process the files inside
    }




    @Override
    protected void processSubDataEntity(MultiValueHashMap<String, Object> subDataEntityInformation, Metadata metadata,
            ContentHandler handler2use4recursiveCall, ParseContext context) throws Exception
    {

        File fSubfile = (File) subDataEntityInformation.getFirst("fileObject");


        URLName url = new URLName(fSubfile.toURI().toURL());

        metadata = URLStreamProvider.getURLStreamProvider(url).addFirstMetadata(url, metadata, context);
        InputStream stream = URLStreamProvider.getURLStreamProvider(url).getStream(url, metadata, context);


        try
        {

            if(m_leech == null) m_leech = new Leech();


            Parser parser = m_leech.getParser();

            parser.parse(stream, handler2use4recursiveCall, metadata, context);

        }
        finally
        {
            if(stream != null) stream.close();
        }
    }







}
