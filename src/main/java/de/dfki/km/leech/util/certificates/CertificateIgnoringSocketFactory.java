package de.dfki.km.leech.util.certificates;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;



/**
 * This is a socket factory that ignores ssl certificates.
 * 
 * TODO refactoring für dieses ganzes package - das ist im Moment noch alles etwas pervers...
 */
public class CertificateIgnoringSocketFactory extends SSLSocketFactory
{

    protected SSLSocketFactory factory;



    /**
     * Creates a socket factory that will ignore the ssl certificate, and accept any as valid.
     * 
     */
    public CertificateIgnoringSocketFactory()
    {
        try
        {
            SSLContext sslcontext = SSLContext.getInstance("TLS");

            sslcontext.init(null, new TrustManager[] { new NaiveTrustManager() }, null);
            factory = sslcontext.getSocketFactory();
        }
        catch (Exception e)
        {
            Logger.getLogger(CertificateIgnoringSocketFactory.class.getName()).log(Level.SEVERE, "Error", e);
        }
    }



    /**
     * Read trusted certificates from the given keyStore
     * 
     * @param certificateFile
     * @param password
     */
    public CertificateIgnoringSocketFactory(File certificateFile, String password)
    {
        try
        {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            // FIXME: obtain trustmanager through TrustManagerRegistry service
            StandardTrustManager trustManager = new StandardTrustManager(certificateFile, password.toCharArray());
            sslcontext.init(null, new TrustManager[] { trustManager }, null);
            factory = sslcontext.getSocketFactory();
        }
        catch (Exception e)
        {
            Logger.getLogger(CertificateIgnoringSocketFactory.class.getName()).log(Level.SEVERE, "Error", e);
        }
    }



    /**
     * Returns the default socket factory
     * 
     * @return the default socket factory
     */
    public static SocketFactory getDefault()
    {
        return new CertificateIgnoringSocketFactory();
    }



    /**
     * Creates a socket
     * 
     * @return a newly created socket
     * @throws IOException if na I/O error occurs
     */
    @Override
    public Socket createSocket() throws IOException
    {
        return factory.createSocket();
    }



    /**
     * Creates a socket with the given parameters.
     * 
     * @param socket the parent socket
     * @param host the host address
     * @param port the port number
     * @param flag the flag
     * @return a newly created socket
     * @throws IOException if something goes wrong in the process
     */
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean flag) throws IOException
    {
        return factory.createSocket(socket, host, port, flag);
    }



    /**
     * Creates a socket with the given parameters.
     * 
     * @param address the internet address
     * @param localAddress the local address
     * @param port the remote port number
     * @param localPort the local port number
     * @return a newly created socket
     * @throws IOException if something goes wrong in the process
     */
    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException
    {
        return factory.createSocket(address, port, localAddress, localPort);
    }



    /**
     * Creates a socket with the given parameters.
     * 
     * @param host the internet address
     * @param port the remote port number
     * @return a newly created socket
     * @throws IOException if something goes wrong in the process
     */
    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException
    {
        return factory.createSocket(host, port);
    }



    /**
     * Creates a socket with the given parameters.
     * 
     * @param host the internet address
     * @param port the remote port number
     * @param localHost the local address
     * @param localPort the local port number
     * @return a newly created socket
     * @throws IOException if something goes wrong in the process
     */
    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException
    {
        return factory.createSocket(host, port, localHost, localPort);
    }



    /**
     * Creates a socket with the given parameters.
     * 
     * @param host the internet address
     * @param port the remote port number
     * @return a newly created socket
     * @throws IOException if something goes wrong in the process
     */
    @Override
    public Socket createSocket(String host, int port) throws IOException
    {
        return factory.createSocket(host, port);
    }



    /**
     * Returns an array of default cipher suites.
     * 
     * @return an array of default cipher suites.
     */
    @Override
    public String[] getDefaultCipherSuites()
    {
        return factory.getDefaultCipherSuites();
    }



    /**
     * Returns an array of supported cipher suites.
     * 
     * @return an array of supported cipher suites.
     */
    @Override
    public String[] getSupportedCipherSuites()
    {
        return factory.getSupportedCipherSuites();
    }




    private static class NaiveTrustManager implements X509TrustManager
    {

        /** Default constructor */
        public NaiveTrustManager()
        {
            // do nothing
        }



        /**
         * Checks if a certificate can be trusted. This naive implementation accepts all certificates.
         * 
         * @see X509TrustManager#checkClientTrusted(X509Certificate[], String)
         */
        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
        {
            // accept everything
        }



        /**
         * Checks if a certificate can be trusted. This naive implementation accepts all certificates.
         * 
         * @see X509TrustManager#checkServerTrusted(X509Certificate[], String)
         */
        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException
        {
            // accept everything
        }



        /**
         * Returns null
         * 
         * @see X509TrustManager#getAcceptedIssuers()
         */
        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }

    }
}
