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



import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.URLName;
import javax.mail.internet.MimeMessage;
import javax.mail.search.FlagTerm;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.dfki.inquisition.collections.MultiValueHashMap;
import de.dfki.inquisition.text.StringUtils;
import de.dfki.km.leech.Leech;
import de.dfki.km.leech.config.CrawlerContext;
import de.dfki.km.leech.config.ImapCrawlerContext;
import de.dfki.km.leech.detect.DatasourceMediaTypes;
import de.dfki.km.leech.io.ImapURLStreamProvider;
import de.dfki.km.leech.io.URLStreamProvider;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.util.ExceptionUtils;
import de.dfki.km.leech.util.certificates.CertificateIgnoringSocketFactory;



/**
 * CrawlerParser implementation for crawling imap servers. The class deals with Metadata.source url of the following form:<br>
 * <br>
 * imap[s]://username:password@hostname:port/folder2crawl<br>
 * <br>
 * In the case no folder is specified but only the server credentials, the server default directory plus the INBOX folder will be crawled
 * 
 * @author Christian Reuschling, Dipl.Ing.(BA)
 */
public class ImapCrawlerParser extends CrawlerParser
{

    private static final long serialVersionUID = 6062546853256504993L;



    static public Store connect2Server(URLName url, ParseContext context) throws MessagingException
    {

        ImapCrawlerContext imapCrawlerContext = context.get(ImapCrawlerContext.class, new ImapCrawlerContext());

        Properties properties = System.getProperties();

        properties.setProperty("mail.store.protocol", url.getProtocol());

        if(imapCrawlerContext.getIgnoreSSLCertificates())
        {
            properties.setProperty("mail.imaps.socketFactory.class", CertificateIgnoringSocketFactory.class.getName());
            properties.setProperty("mail.imaps.socketFactory.fallback", "false");
        }

        if(!StringUtils.nullOrWhitespace(imapCrawlerContext.getSSLCertificateFilePath()) && "imaps".equalsIgnoreCase(url.getProtocol()))
        {
            properties.setProperty("javax.net.ssl.trustStore", imapCrawlerContext.getSSLCertificateFilePath());
            properties.setProperty("javax.net.ssl.trustStorePassword", imapCrawlerContext.getSSLCertificateFilePassword());
        }


        Session session = Session.getDefaultInstance(properties);
        Store mailStore = session.getStore(url.getProtocol());


        String strUserName = imapCrawlerContext.getUserName();
        if(strUserName == null) strUserName = url.getUsername();

        String strPassword = imapCrawlerContext.getPassword();
        if(strPassword == null) strPassword = url.getPassword();

        if(!mailStore.isConnected()) mailStore.connect(url.getHost(), url.getPort(), strUserName, strPassword);


        return mailStore;
    }



    /**
     * Does this folder hold any subfolders?
     * 
     * @param folder the folder to be checked
     * @return true if this folder has any subfolders, false otherwise
     * @throws MessagingException if it prooves impossible to find out
     */
    public static boolean holdsFolders(Folder folder) throws MessagingException
    {
        // this if has been added during the work on issue 2005759
        // gmail returns wrong type, it is necessary to call list() to determine
        // if a folder actually contains subfolders
        if((folder.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS)
        {
            return folder.list().length > 0;
        }
        else
        {
            // this means that the folder can't have any subfolders "by definition"
            return false;
        }
    }




    /**
     * Does this folder hold any messages?
     * 
     * @param folder the folder to be checked
     * @return true if this folder has any messages, false otherwise
     * @throws MessagingException if it prooves impossible to find out
     */
    public static boolean holdsMessages(Folder folder) throws MessagingException
    {
        return (folder.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES;
    }



    protected HashMap<Folder, Boolean> m_hsImapFolder2Stickyness = new HashMap<Folder, Boolean>();


    protected Leech m_leech;



    protected Store m_mailStore;



    protected boolean checkIfInConstraints(String strURL2Check, MimeMessage message, ParseContext context) throws MessagingException
    {
        CrawlerContext crawlerContext = context.get(CrawlerContext.class, new CrawlerContext());


        if(!crawlerContext.getURLFilter().accept(strURL2Check))
        {
            String strType = "IMAP directory ";
            if(message != null) strType = "IMAP message ";

            if(crawlerContext.getVerbose())
                Logger.getLogger(CrawlerParser.class.getName()).info(
                        strType + strURL2Check + " is outside the URL constraints for this data source. Skipping.");

            return false;
        }



        return true;
    }



    protected URLName getMessageUrl(Folder folderOfmessage, MimeMessage message) throws MessagingException
    {
        String strUrlName4Folder = folderOfmessage.getURLName().toString();
        if(!strUrlName4Folder.endsWith("/")) strUrlName4Folder += "/";

        // hier ist es bums, ob die id sticky ist oder nicht. Die URL ist der Pointer auf diese, momentane message, mit dem ich die erreichen und
        // downloaden kann. Fur das inkrementelle indexieren ist dann die dataExistsId relevant, die darf dann NICHT diese Url sein, wenn diese nicht
        // sticky ist. in diesem Fall (oder vielleicht sogar immer, mal schauen) nehme ich irgendwelche Daten aus dem header (wie wärs mit
        // folder+messageid)


        return new URLName(strUrlName4Folder + ";UID=" + ((UIDFolder) folderOfmessage).getUID(message));

        // if(uidsAreSticky(folderOfmessage))
        // {
        //
        // }
        // else
        // {
        // if(useHeadersHash)
        // {
        // return strUrlName4Folder + MailUtil.getMessageIdWithHeadersHash(message);
        // }
        // else
        // {
        // return strUrlName4Folder + MailUtil.getMessageId((MimeMessage) message);
        // }
        // }
    }



    @Override
    protected Iterator<MultiValueHashMap<String, Object>> getSubDataEntitiesInformation(InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context) throws Exception
    {

        // imap url schema: imap[s]://uname@hostname:port/folder;uidvalidity=385759045/;uid=20. Examples (incl. message-referenzierung)
        // http://xml.resource.org/public/rfc/html/rfc2192.html#anchor10
        // allerdings nimmt der Java ImapStore auch URLs mit Passwörtern an. Dann geht auch
        // imap[s]://uname:pwd@hostname:port/folder;uidvalidity=385759045/;uid=20


        CrawlerContext crawlerContext = context.get(CrawlerContext.class, new CrawlerContext());


        String strContainerURL = metadata.get(Metadata.SOURCE);

        URLName containerURLName = new URLName(strContainerURL);

        if(m_mailStore == null) m_mailStore = connect2Server(containerURLName, context);

        // wenn kein directory angegeben wird, dann crawlen wir einfach den default folder und die inbox
        LinkedList<Folder> llFolderz2Crawl = new LinkedList<Folder>();
        if(containerURLName.getFile() != null)
        {
            Folder folder = m_mailStore.getFolder(containerURLName.getFile());
            if(folder != null && folder.exists()) llFolderz2Crawl.add(folder);
             else
                 throw new FileNotFoundException("Can't find imap folder '" + folder.getFullName() + "'");
            
        }
        else
        {
            Folder folder = m_mailStore.getDefaultFolder();
            if(folder != null && folder.exists()) llFolderz2Crawl.add(folder);

            folder = m_mailStore.getFolder("INBOX");
            if(folder != null && folder.exists()) llFolderz2Crawl.add(folder);
        }



        LinkedList<MultiValueHashMap<String, Object>> llEntityInfo = new LinkedList<MultiValueHashMap<String, Object>>();


        for (Folder folder2crawl : llFolderz2Crawl)
        {
            // Jetzt haben wir die Containerobjekte - nun geben wir die Daten zu den SubEntities zurück


            // die subfolder
            boolean bFolderCanHaveSubFolders = (folder2crawl.getType() & Folder.HOLDS_FOLDERS) == Folder.HOLDS_FOLDERS;

            if(bFolderCanHaveSubFolders)
            {
                folder2crawl.open(Folder.READ_ONLY);


                Folder[] subFolders = folder2crawl.list();
                for (Folder subFolder : subFolders)
                {
                    URLName urlName = subFolder.getURLName();
                    URLName urlNameWithPassword =
                            new URLName(containerURLName.getProtocol(), urlName.getHost(), urlName.getPort(), urlName.getFile(),
                                    urlName.getUsername(), containerURLName.getPassword());

                    if(!checkIfInConstraints(urlName.toString(), null, context)) continue;


                    MultiValueHashMap<String, Object> hsEntityInformation = new MultiValueHashMap<String, Object>();

                    hsEntityInformation.add(CrawlerParser.SOURCEID, urlName);
                    hsEntityInformation.add("urlNameWithPassword", urlNameWithPassword);
                    hsEntityInformation.add("folder", subFolder.getFullName());

                    llEntityInfo.add(hsEntityInformation);
                }
            }


            // die messages
            boolean bFolderCanHaveMessages = (folder2crawl.getType() & Folder.HOLDS_MESSAGES) == Folder.HOLDS_MESSAGES;

            if(bFolderCanHaveMessages)
            {
                if(!folder2crawl.isOpen()) folder2crawl.open(Folder.READ_ONLY);


                // wir holen uns alle nicht-deleted messages, und werfen noch die raus, die 'expunged' sind
                Message[] relevantMessagesOfFolder = folder2crawl.search(new FlagTerm(new Flags(Flags.Flag.DELETED), false));
                ArrayList<Message> nonDelNonExpungedMessages = new ArrayList<Message>();
                for (Message message : relevantMessagesOfFolder)
                    if(!message.isExpunged()) nonDelNonExpungedMessages.add(message);
                relevantMessagesOfFolder = nonDelNonExpungedMessages.toArray(new Message[0]);

                // die Daten die wir später benötigen holen wir uns effizient in einem Rutsch - deswegen benötigen wir auch keinen Thread mit dem
                // OneAfterOneIterator, um Speicher zu sparen (siehe DirectoryCrawlerParser). Das Array haben wir hier eh. Entweder oder.
                FetchProfile profile = new FetchProfile();
                profile.add(UIDFolder.FetchProfileItem.UID);
                profile.add("Message-ID");
                folder2crawl.fetch(relevantMessagesOfFolder, profile);


                for (int i = 0; i < relevantMessagesOfFolder.length && !crawlerContext.stopRequested(); i++)
                {
                    MimeMessage message = (MimeMessage) relevantMessagesOfFolder[i];

                    // hier brauchen wir noch eine URL mit und eine ohne Passwort
                    URLName urlName = getMessageUrl(folder2crawl, message);
                    URLName urlNameWithPassword =
                            new URLName(containerURLName.getProtocol(), urlName.getHost(), urlName.getPort(), urlName.getFile(),
                                    urlName.getUsername(), containerURLName.getPassword());


                    if(!checkIfInConstraints(urlName.toString(), message, context)) continue;


                    MultiValueHashMap<String, Object> hsEntityInformation = new MultiValueHashMap<String, Object>();

                    hsEntityInformation.add(CrawlerParser.SOURCEID, urlName);
                    hsEntityInformation.add("urlNameWithPassword", urlNameWithPassword);
                    hsEntityInformation.add("Message-ID", message.getHeader("Message-ID")[0]);
                    hsEntityInformation.add("folder", folder2crawl.getFullName());

                    llEntityInfo.add(hsEntityInformation);
                }
            }

            // wir haben die folder abgearbeitet, dann können wir diesen Speicher wieder frei geben
            m_hsImapFolder2Stickyness.clear();





            if(folder2crawl.isOpen()) folder2crawl.close(false);
        }



        return llEntityInfo.iterator();
    }



    @Override
    public Set<MediaType> getSupportedTypes(ParseContext context)
    {
        return Collections.singleton(DatasourceMediaTypes.IMAPFOLDER);
    }



    @Override
    public void parse(InputStream stream, ContentHandler handler, Metadata metadata, ParseContext context) throws IOException, SAXException,
            TikaException
    {
        super.parse(stream, handler, metadata, context);


        // Wenn ein completter crawl fertig ist, dann schliessen wir auch wieder unseren MailStore

        int iCurrentCrawlingDepth = 0;
        String strDepth = metadata.get(CrawlerParser.CURRENT_CRAWLING_DEPTH);
        if(strDepth != null) iCurrentCrawlingDepth = Integer.valueOf(strDepth);


        if(iCurrentCrawlingDepth != 0) return;

        try
        {

            m_mailStore.close();
            m_mailStore = null;

        }
        catch (MessagingException e)
        {
            String strSourceID = metadata.get(Metadata.SOURCE);

            ExceptionUtils.handleException(e, strSourceID, metadata, context.get(CrawlerContext.class, new CrawlerContext()), context,
                    iCurrentCrawlingDepth, handler);
        }
    }



    @Override
    protected void processCurrentDataEntity(InputStream stream, Metadata metadata, ContentHandler handler, ParseContext context) throws Exception
    {
        // NOP - wie don't process directories - we only process the files inside
    }



    @Override
    protected void processSubDataEntity(MultiValueHashMap<String, Object> subDataEntityInformation, Metadata metadata,
            ContentHandler handler2use4recursiveCall, ParseContext context) throws Exception
    {

        URLName urlNameWithPassword = (URLName) subDataEntityInformation.getFirst("urlNameWithPassword");

        String strMessageId = (String) subDataEntityInformation.getFirst("Message-ID");
        String strMessageFolder = (String) subDataEntityInformation.getFirst("folder");

        String strEntityId = ImapURLStreamProvider.getEntityId(strMessageFolder, strMessageId);

        // wir setzten die hier schon mal - die Daten haben wir in einem prefetching-Schritt schon effizient geladen. Wenn diese hier schon im
        // Metadata-Objekt stehen, werden sie von der addFirstMetadata nicht nochmal geladen
        metadata.set(Metadata.SOURCE, urlNameWithPassword.toString());
        metadata.set(IncrementalCrawlingHistory.dataEntityId, strEntityId);
        metadata.set(IncrementalCrawlingHistory.dataEntityContentFingerprint,
                ImapURLStreamProvider.getDataEntityContentFingerprint(strEntityId));
        URLName urlNameWithoutPassword =
                new URLName(urlNameWithPassword.getProtocol(), urlNameWithPassword.getHost(), urlNameWithPassword.getPort(),
                        urlNameWithPassword.getFile(), urlNameWithPassword.getUsername(), "");
        metadata.set(Metadata.RESOURCE_NAME_KEY, urlNameWithoutPassword.toString());
        if(strMessageId == null)
            metadata.set("Content-Type", DatasourceMediaTypes.IMAPFOLDER.toString());
        else
            metadata.set("Content-Type", "message/rfc822");



        metadata =
                URLStreamProvider.getURLStreamProvider4Protocol(urlNameWithPassword.getProtocol()).addFirstMetadata(urlNameWithPassword, metadata,
                        context);
        InputStream stream = URLStreamProvider.getURLStreamProvider(urlNameWithPassword).getStream(urlNameWithPassword, metadata, context);

        try
        {

            if(m_leech == null) m_leech = new Leech();


            // hier nimmt der dann bei einer message hoffentlich den Tika RFC822Parser
            Parser parser = m_leech.getParser();

            parser.parse(stream, handler2use4recursiveCall, metadata, context);

        }
        finally
        {
            if(stream != null) stream.close();
        }

    }




}
