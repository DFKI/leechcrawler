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

package de.dfki.km.leech.io;



import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * This is a wrapper for an InputStream, where the construction time of the wrapped InputStream will be shifted to the first data access on the stream
 * wrapper. Thus, you don't have any computation time, even for object construction/initialization in the case the wrapped InputStream won't be read
 * out / used. Further, you can hook at initialization time (before or after wrapped stream creation time at
 * {@link #initBeforeFirstStreamDataAccess()}. Of corse you can also hook at closing time by implemention the {@link #close()} method.
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 * 
 */
public abstract class ShiftInitInputStream extends InputStream
{

    protected InputStream m_wrappedInputStream;




    public ShiftInitInputStream()
    {
    }



    /**
     * Gets the wrapped InputStream Object, as created inside {@link #initBeforeFirstStreamDataAccess()}
     * 
     * @return the wrapped InputStream Object, as created inside {@link #initBeforeFirstStreamDataAccess()}
     */
    public InputStream getWrappedStream()
    {
        return m_wrappedInputStream;
    }



    @Override
    public int available() throws IOException
    {
        try
        {

            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }


        return m_wrappedInputStream.available();
    }



    @Override
    public void close() throws IOException
    {
        if(m_wrappedInputStream != null) m_wrappedInputStream.close();
    }



    /**
     * This method will be called the first time one of the methods {@link #available()}, {@link #mark(int)}, {@link #markSupported()},
     * {@link #read()}, {@link #read(byte[])}, {@link #read(byte[], int, int)}, {@link #reset()} or {@link #skip(long)} will be invoked, for
     * initialization. This is where you have to create the wrapped stream object, to make sure that computation time for stream object construction
     * will be consumed at first data access.
     * 
     * @return the inputstream that should be wrapped
     * 
     * @throws Exception
     */
    abstract protected InputStream initBeforeFirstStreamDataAccess() throws Exception;



    @Override
    public synchronized void mark(int readlimit)
    {
        try
        {


            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

            m_wrappedInputStream.mark(readlimit);

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }
    }



    @Override
    public boolean markSupported()
    {
        try
        {

            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }


        return m_wrappedInputStream.markSupported();
    }



    @Override
    public int read() throws IOException
    {
        try
        {

            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }


        return m_wrappedInputStream.read();
    }



    @Override
    public int read(byte[] b) throws IOException
    {
        try
        {
            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }


        return m_wrappedInputStream.read(b);
    }



    @Override
    public int read(byte[] b, int off, int len) throws IOException
    {
        try
        {

            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }


        return m_wrappedInputStream.read(b, off, len);
    }



    @Override
    public synchronized void reset() throws IOException
    {
        try
        {

            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }


        m_wrappedInputStream.reset();
    }



    @Override
    public long skip(long n) throws IOException
    {
        try
        {

            if(m_wrappedInputStream == null) m_wrappedInputStream = initBeforeFirstStreamDataAccess();

            if(m_wrappedInputStream == null)
                throw new NullPointerException(
                        "The inputStream you want to wrap is not initialized. Create it inside the implementation of initBeforFirstStreamDataAccess().");

        }
        catch (Exception e)
        {
            Logger.getLogger(ShiftInitInputStream.class.getName()).log(Level.SEVERE, "Error", e);
        }


        return m_wrappedInputStream.skip(n);
    }




}
