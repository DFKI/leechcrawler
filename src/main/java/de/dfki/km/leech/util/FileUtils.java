package de.dfki.km.leech.util;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;



/**
 * Some little helpful utils, regarding files. <br>
 * <br>
 * <b>CAUTION: The english vocabulary and grammar comes WITHOUT ANY WARRANTY. I did my best, with an appropriate amount of time :o) </b> <br>
 * 
 * @author Christian Reuschling
 */
public class FileUtils
{
    /**
     * Appends the content behind an InputStream to a file. The InputStream will be closed afterwards.
     * 
     * @param isContent2Append the file content to append as InputStream
     * @param strFileName the path of the (new) file
     * 
     * @throws Exception
     */
    static public void append2File(InputStream isContent2Append, String strFileName) throws Exception
    {
        OutputStream out = new FileOutputStream(new File(strFileName), true);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;

        while ((len = isContent2Append.read(buf)) > 0)
            out.write(buf, 0, len);


        out.flush();

        isContent2Append.close();
        out.close();
    }






    /**
     * Appends a string to a file. The character encoding can be specified. In the case the file don't exists, it will be created.
     * Possible encoding values are e.g. <br>
     * US-ASCII Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set <br>
     * ISO-8859-1 ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1 <br>
     * UTF-8 Eight-bit UCS Transformation Format <br>
     * UTF-16BE Sixteen-bit UCS Transformation Format, big-endian byte order <br>
     * UTF-16LE Sixteen-bit UCS Transformation Format, little-endian byte order <br>
     * UTF-16 Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark <br>
     * 
     * @param strContent2Append the String that should be appended to the file
     * @param strFileName the path of the (new) file
     * @param strCharEncoding the character encoding as String
     * 
     * @throws Exception
     */
    static public void append2File(String strContent2Append, String strFileName, String strCharEncoding) throws Exception
    {
        File fNewFile = new File(strFileName);

        File fParent = fNewFile.getParentFile();
        if(fParent != null) fParent.mkdirs();

        Charset charset = Charset.forName(strCharEncoding);

        FileOutputStream fos = new FileOutputStream(fNewFile, true);
        OutputStreamWriter oos = new OutputStreamWriter(fos, charset);

        oos.write(strContent2Append);
        oos.flush();
        oos.close();
        fos.flush();
        fos.close();

    }



    /**
     * Copies one File to another. If the destination file does not exist, it will be created
     * 
     * @param fSource the file to copy
     * @param fDestination the destination file
     * @throws IOException
     */
    static public void copyFile(File fSource, File fDestination) throws IOException
    {
        InputStream in = new FileInputStream(fSource);
        OutputStream out = new FileOutputStream(fDestination);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;

        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);


        out.flush();

        in.close();
        out.close();
    }



    /**
     * Deletes all files and subdirectories under dir. Returns true if all deletions were successful. If a deletion fails, the method stops attempting
     * to delete and returns false.
     * 
     * @param dir2delete the directory that should be removed.
     * 
     * @return true in the case the deletion was successfull, false otherwise
     */
    static public boolean deleteDirectory(File dir2delete)
    {

        if(dir2delete.isDirectory())
        {
            String[] childs = dir2delete.list();
            for (int i = 0; i < childs.length; i++)
            {
                boolean bWasDeleted = deleteDirectory(new File(dir2delete, childs[i]));

                if(!bWasDeleted) return false;
            }
        }

        // The directory is now empty so delete it
        return dir2delete.delete();
    }






    /**
     * Returns the content of a file as string. This method uses UTF-8 encoding.
     * 
     * @param strPath the file path
     * 
     * @return the file content as string
     * 
     * @throws Exception
     */
    static public String file2String(String strPath) throws Exception
    {
        return FileUtils.file2String(strPath, "UTF-8");
    }



    /**
     * Returns the content of a file as string. The character encoding can be specified. Possible values are e.g. <br>
     * US-ASCII Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set <br>
     * ISO-8859-1 ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1 <br>
     * UTF-8 Eight-bit UCS Transformation Format <br>
     * UTF-16BE Sixteen-bit UCS Transformation Format, big-endian byte order <br>
     * UTF-16LE Sixteen-bit UCS Transformation Format, little-endian byte order <br>
     * UTF-16 Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark <br>
     * 
     * @param strPath the file path
     * @param strCharEncoding the character encoding as String
     * @return the file content as string
     * @throws IOException
     */
    static public String file2String(String strPath, String strCharEncoding) throws IOException
    {

        File file = new File(strPath).getAbsoluteFile();

        byte[] bytes = FileUtils.getBytesFromFile(file);

        String strContent = new String(bytes, strCharEncoding);


        return strContent;
    }



    /**
     * Gets all file objects recursively that are under this directory
     * 
     * @param directory the root directory
     * @return all File objects that are under this directory, also in subdirectories
     */
    public static ArrayList<File> getAllDirsAndFiles(File directory)
    {
        ArrayList<File> alDirsAndFiles = new ArrayList<File>();

        if(directory.isDirectory())
        {
            String[] children = directory.list();

            for (int i = 0; i < children.length; i++)
            {
                File fChild = new File(directory, children[i]);

                alDirsAndFiles.add(fChild);

                alDirsAndFiles.addAll(getAllDirsAndFiles(fChild));

            }
        }
        // zurückgeben meiner Kinder und der rekursiven Kinder
        return alDirsAndFiles;
    }



    // Returns the contents of the file in a byte array.
    public static byte[] getBytesFromFile(File file) throws IOException
    {
        InputStream is = new FileInputStream(file);

        // Get the size of the file
        long length = file.length();

        // You cannot create an array using a long type.
        // It needs to be an int type.
        // Before converting to an int type, check
        // to ensure that file is not larger than Integer.MAX_VALUE.
        if(length > Integer.MAX_VALUE)
        {
            // File is too large
        }

        // Create the byte array to hold the data
        byte[] bytes = new byte[(int) length];

        // Read in the bytes
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0)
        {
            offset += numRead;
        }

        // Ensure all the bytes have been read in
        if(offset < bytes.length)
        {
            throw new IOException("Could not completely read file " + file.getName());
        }

        // Close the input stream and return bytes
        is.close();
        return bytes;
    }





    /**
     * Writes the content behind an InputStream to a file. The InputStream will be closed afterwards.
     * 
     * @param isContent the new file content as InputStream
     * @param strFileName the path of the (new) file
     * 
     * @throws Exception
     */
    static public void inputStream2File(InputStream isContent, String strFileName) throws Exception
    {
        OutputStream out = new FileOutputStream(new File(strFileName));

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;

        while ((len = isContent.read(buf)) > 0)
            out.write(buf, 0, len);


        out.flush();

        isContent.close();
        out.close();
    }






    /**
     * Writes a string into a file. This method uses UTF-8 encoding
     * 
     * @param strContent the new file content as String
     * @param strFileName the path of the (new) file
     * 
     * @throws Exception
     */
    static public void string2File(String strContent, String strFileName) throws Exception
    {
        string2File(strContent, strFileName, "UTF-8");
    }




    /**
     * Writes a string into a file. The character encoding can be specified. Possible values are e.g. <br>
     * US-ASCII Seven-bit ASCII, a.k.a. ISO646-US, a.k.a. the Basic Latin block of the Unicode character set <br>
     * ISO-8859-1 ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1 <br>
     * UTF-8 Eight-bit UCS Transformation Format <br>
     * UTF-16BE Sixteen-bit UCS Transformation Format, big-endian byte order <br>
     * UTF-16LE Sixteen-bit UCS Transformation Format, little-endian byte order <br>
     * UTF-16 Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark <br>
     * 
     * @param strContent the new file content as String
     * @param strFileName the path of the (new) file
     * @param strCharEncoding the character encoding as String
     * 
     * @throws Exception
     */
    static public void string2File(String strContent, String strFileName, String strCharEncoding) throws Exception
    {

        File fNewFile = new File(strFileName);

        File fParent = fNewFile.getParentFile();
        if(fParent != null) fParent.mkdirs();

        Charset charset = Charset.forName(strCharEncoding);

        FileOutputStream fos = new FileOutputStream(fNewFile);
        OutputStreamWriter oos = new OutputStreamWriter(fos, charset);

        oos.write(strContent);
        oos.flush();
        oos.close();
        fos.flush();
        fos.close();
    }







}
