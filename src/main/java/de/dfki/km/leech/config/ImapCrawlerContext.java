package de.dfki.km.leech.config;






public class ImapCrawlerContext
{




    protected boolean m_ignoreSSLCertificates = true;

    protected String m_password;

    protected String m_sslCertificateFilePassword;

    protected String m_sslCertificateFilePath;

    protected String m_userName;



    /**
     * Gets whether the SSL certificates should be ignored for connection or not
     * 
     * @return true in the case the SSL certificates should be ignored, false otherwise. Default is true.
     */
    public boolean getIgnoreSSLCertificates()
    {
        return m_ignoreSSLCertificates;
    }



    /**
     * Gets the password for connecting the imap server
     * 
     * @return the password for connecting the imap server
     */
    public String getPassword()
    {
        return m_password;
    }



    /**
     * Returns the Keyphrase for the SSL keyfile
     * 
     * @return the Keyphrase for the SSL keyfile
     */
    public String getSSLCertificateFilePassword()
    {
        return m_sslCertificateFilePassword;
    }



    /**
     * Returns the path to the ssl keyfile
     * 
     * @return the path to the ssl keyfile
     */
    public String getSSLCertificateFilePath()
    {
        return m_sslCertificateFilePath;
    }




    /**
     * Gets the username for connecting the imap server
     * 
     * @return the username for connecting the imap server
     */
    public String getUserName()
    {
        return m_userName;
    }







    /**
     * Sets whether the SSL certificates should be ignored for connection or not
     * 
     * @param ignoreSSLCertificates true in the case the SSL certificates should be ignored (this is the default)
     * 
     * @return this for convenience
     */
    public ImapCrawlerContext setIgnoreSSLCertificates(boolean ignoreSSLCertificates)
    {
        m_ignoreSSLCertificates = ignoreSSLCertificates;

        return this;
    }







    /**
     * Sets the password for connecting the imap server
     * 
     * @param password the password for connecting the imap serverthe password for connecting the imap server
     * 
     * @return this for convenience
     */
    public ImapCrawlerContext setPassword(String password)
    {
        this.m_password = password;

        return this;
    }





    /**
     * Sets the Keyphrase for the SSL keyfile
     * 
     * @param sslFilePassword Keyphrase for the SSL keyfile
     * 
     * @return this for convenience
     */
    public ImapCrawlerContext setSSLCertificateFilePassword(String sslFilePassword)
    {
        this.m_sslCertificateFilePassword = sslFilePassword;

        return this;
    }






    /**
     * Sets the path to the ssl keyfile
     * 
     * @param sslCertificateFilePath the path to the ssl keyfile
     * 
     * @return this for convenience
     */
    public ImapCrawlerContext setSSLCertificateFilePath(String sslCertificateFilePath)
    {
        this.m_sslCertificateFilePath = sslCertificateFilePath;

        return this;
    }



    /**
     * Sets the username for connecting the imap server
     * 
     * @param userName the username for connecting the imap server
     * 
     * @return this for convenience
     */
    public ImapCrawlerContext setUserName(String userName)
    {
        this.m_userName = userName;

        return this;
    }



}
