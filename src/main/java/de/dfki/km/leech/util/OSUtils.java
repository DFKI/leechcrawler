package de.dfki.km.leech.util;



import java.io.File;
import java.io.IOException;



public class OSUtils
{

    public static boolean isWindows()
    {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }



    public static boolean isMac()
    {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }



    public static boolean isLinux()
    {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }



    /**
     * Checks if the given file is a MacOS X bundle. This only makes sense on MaxOS X, and will always
     * return false on other OS. In the case the given file is no directory, the method will also return false.
     * 
     * @param fDir2Check the file to check
     * 
     * @return true in the case it is an macOS X bundle
     */
    public static boolean isMacOSXBundle(File fDir2Check)
    {
        if(!fDir2Check.isDirectory()) return false;

        if(!isMac()) return false;

        try
        {
            return isBundle(fDir2Check);
        }
        catch (IOException e)
        {
            return false;
        }

    }


    final static private String[] bundleextension = new String[] { ".app", ".bundle", ".framework", ".kext", ".mpkg", "mdimporter", ".nib",
            ".pbproj", ".pkg", ".plugin", ".prefPane", ".rtfd", ".saver", ".slideSaver", ".wdgt", ".webarchive", ".xcode", ".xcodeproj", ".key",
            ".pages" };



    /**
     * This returns true if the file parameter is a "bundle"
     * I've found no mac library to detect this, so it's based on file extension, which is sub-optimal.
     * 
     * .app - Application bundle (com.apple.application-���bundle)
     * .bundle - Generic bundle (com.apple.bundle)
     * .framework - Framework bundle (com.apple.framework)
     * .kext - Kernel EXTension?
     * .mpkg - see Archives, Disk Images, Compression
     * .mdimporter - Spotlight Metadata Importer (com.apple.metadata-���importer)
     * .nib - NeXT Interface Builder
     * .pbproj - ProjectBuilder project (also openable by XCode; see also .xcode)
     * .pkg - see Archives, Disk Images, Compression
     * .plugin - Plugin bundle (com.apple.plugin)
     * .prefPane - System Preferences pane bundle
     * .rtfd - See Text Files
     * .saver - Screensaver bundle
     * .slideSaver - Slideshow screensaver bundle (with embedded images)
     * .wdgt - Dashboard widget (com.apple.dashboard-���widget)
     * .webarchive - Safari web archive
     * .xcode - XCode project (version 2.0 and earlier)
     * .xcodeproj - XCode project (version 2.1 and later)
     * 
     * .pages - pages document
     * .key - keynot document
     * 
     * @param f
     * @throws IOException
     */
    private static boolean isBundle(File f) throws IOException
    {
        if(f.isDirectory())
        {
            String filename = f.getName();
            for (String e : bundleextension)
            {
                if(filename.endsWith(e)) return true;
            }
        }
        return false;
    }
}
