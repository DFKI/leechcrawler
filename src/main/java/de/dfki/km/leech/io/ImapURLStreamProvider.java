/*
 * Leech - crawling capabilities for Apache Tika
 * 
 * Copyright (C) 2012 DFKI GmbH, Author: Christian Reuschling
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contact us by mail: christian.reuschling@dfki.de
 */

package de.dfki.km.leech.io;



import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;
import javax.mail.URLName;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.DublinCore;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

import de.dfki.km.leech.detect.DatasourceMediaTypes;
import de.dfki.km.leech.parser.ImapCrawlerParser;
import de.dfki.km.leech.parser.incremental.IncrementalCrawlingHistory;
import de.dfki.km.leech.util.UrlUtil;



public class ImapURLStreamProvider extends URLStreamProvider
{

    public static String getDataEntityContentFingerprint(String strEntityExistsId)
    {
        // XXX wir gehen hier davon aus, daß eine message nicht modifiziert werden kann - allerdings kann man sehr wohl z.B. ein attachment
        // löschen. Man könnte hier noch die attachment-Liste mit reinpacken (ich weiß nur gerade nicht wie^^)

        return strEntityExistsId;
    }





    public static String getEntityExistsId(String strFolderOfMessage, String strMessageId)
    {
        if(strMessageId == null) return strFolderOfMessage;

        return strFolderOfMessage + " MessageId " + strMessageId;
    }



    @SuppressWarnings("null")
    @Override
    public Metadata addFirstMetadata(URLName url2getMetadata, Metadata metadata2fill, ParseContext parseContext) throws Exception
    {

        if(metadata2fill == null) metadata2fill = new Metadata();


        // wenn das Teil schon gefüllt ist, dann machen wir gar nix
        if(!(metadata2fill.get(Metadata.SOURCE) == null || metadata2fill.get(IncrementalCrawlingHistory.dataEntityExistsID) == null
                || metadata2fill.get(IncrementalCrawlingHistory.dataEntityContentFingerprint) == null
                || metadata2fill.get(Metadata.RESOURCE_NAME_KEY) == null || metadata2fill.get("Content-Type") == null))
        {
            // alle sind bereits gesetzt
            return metadata2fill;
        }


        Store mailStore = ImapCrawlerParser.connect2Server(url2getMetadata, parseContext);
        URLName urlNameWithPassword = url2getMetadata;
        IMAPFolder folder = (IMAPFolder) mailStore.getFolder(urlNameWithPassword.getFile());




        // Für Leech
        metadata2fill.set(Metadata.SOURCE, url2getMetadata.toString());


        // Für das inkrementelle indexieren


        try
        {

            // folder+messageId, damit sich die uid auch zwischen den crawls ändern darf
            String strEntityExistsId = null;
            String strDataEntityContentFingerprint = null;
            if(folder != null && folder.exists())
            {
                // das Teil ist ein Folder
                strEntityExistsId = url2getMetadata.getFile();
                strDataEntityContentFingerprint = strEntityExistsId;

                metadata2fill.set("Content-Type", DatasourceMediaTypes.IMAPFOLDER.toString());
            }
            else if(folder != null)
            {
                // ist das Teil eine message? Wir popeln mal den folder und die uid raus
                if(folder.isOpen()) folder.close(false);

                String strFolder = UrlUtil.extractFolder(url2getMetadata);
                folder = (IMAPFolder) mailStore.getFolder(strFolder);

                if(!folder.isOpen()) folder.open(Folder.READ_ONLY);


                String strUID = UrlUtil.extractUID(url2getMetadata);
                if(strUID == null) throw new FileNotFoundException("no message uid found");

                Message message = folder.getMessageByUID(Long.valueOf(strUID));

                String strMessageId = null;
                try
                {
                    // das message-Objekt holt sich die Inhalte on the fly und cached sie nach dem ersten laden. Das ist hübsch
                    strMessageId = message.getHeader("Message-ID")[0];
                }
                catch (Exception e)
                {
                    throw new IllegalStateException("imap message has no Message-ID header entry");
                }


                strEntityExistsId = getEntityExistsId(strFolder, strMessageId);
                strDataEntityContentFingerprint = getDataEntityContentFingerprint(strEntityExistsId);

                metadata2fill.set("Content-Type", "message/rfc822");


                // XXX machen wir noch eine Möglichkeit mit der MessageID und search (für den Sven, auch wenn die eben nicht eindeutig ist?
                // Vielleicht Folder+MessageId? Oder isch des einfach falsch...andererseits nehmen wir folder+messageId ja auch für den
                // ContenFingerprint, oder?)
            }





            metadata2fill.set(IncrementalCrawlingHistory.dataEntityExistsID, strEntityExistsId);
            metadata2fill.set(IncrementalCrawlingHistory.dataEntityContentFingerprint, strDataEntityContentFingerprint);


            // Für Tika

            URLName urlNameWithoutPassword =
                    new URLName(urlNameWithPassword.getProtocol(), urlNameWithPassword.getHost(), urlNameWithPassword.getPort(),
                            urlNameWithPassword.getFile(), urlNameWithPassword.getUsername(), "");
            metadata2fill.set(Metadata.RESOURCE_NAME_KEY, urlNameWithoutPassword.toString());







            return metadata2fill;
        }
        finally
        {

            if(folder != null && folder.isOpen()) folder.close(false);
            if(mailStore != null && mailStore.isConnected()) mailStore.close();
        }

    }


    static HashMap<String, Store> m_hsHost2Store = new HashMap<String, Store>();



    @Override
    public TikaInputStream getStream(final URLName url2getStream, final Metadata metadata, final ParseContext parseContext) throws Exception
    {

        return TikaInputStream.get(new ShiftInitInputStream()
        {
            IMAPFolder m_folderOfMessage;

            boolean m_bCloseStore = false;



            @Override
            protected InputStream initBeforeFirstStreamDataAccess() throws Exception
            {

                Store mailStore = m_hsHost2Store.get(url2getStream.getHost() + " usr: " + url2getStream.getUsername());
                if(mailStore == null)
                {
                    mailStore = ImapCrawlerParser.connect2Server(url2getStream, parseContext);
                    m_hsHost2Store.put(url2getStream.getHost() + " usr: " + url2getStream.getUsername(), mailStore);
                    if(metadata.get("currentCrawlingDepth") == null || "0".equals(metadata.get("currentCrawlingDepth"))) m_bCloseStore = true;
                }
                Folder folder2Check = mailStore.getFolder(url2getStream.getFile());

                try
                {

                    if(folder2Check != null && folder2Check.exists())
                    {
                        // das Teil ist ein Folder
                        return TikaInputStream.get("leech sucks - hopefully :)".getBytes());
                    }
                    else if(folder2Check != null)
                    {
                        // ist das Teil eine message? Wir popeln mal den folder und die uid raus
                        String strFolder = UrlUtil.extractFolder(url2getStream);
                        m_folderOfMessage = (IMAPFolder) mailStore.getFolder(strFolder);

                        if(!m_folderOfMessage.isOpen()) m_folderOfMessage.open(Folder.READ_ONLY);

                        String strUID = UrlUtil.extractUID(url2getStream);
                        if(strUID == null) throw new FileNotFoundException("no message uid found");


                        final IMAPMessage message = (IMAPMessage) m_folderOfMessage.getMessageByUID(Long.valueOf(strUID));

                        return message.getMimeStream();
                    }

                    return null;


                }
                finally
                {
                    if(folder2Check.isOpen()) folder2Check.close(false);
                }




            }



            @Override
            public void close() throws IOException
            {
                try
                {

                    super.close();

                    if(m_folderOfMessage != null && m_folderOfMessage.isOpen()) m_folderOfMessage.close(false);

                    Store mailStore = m_hsHost2Store.get(url2getStream.getHost() + " usr: " + url2getStream.getUsername());

                    if(mailStore != null && mailStore.isConnected() && m_bCloseStore)
                    {
                        mailStore.close();
                        m_hsHost2Store.remove(url2getStream.getHost() + " usr: " + url2getStream.getUsername());
                    }

                }
                catch (MessagingException e)
                {
                    Logger.getLogger(ImapURLStreamProvider.class.getName()).log(Level.SEVERE, "Error", e);
                }
            }
        });



    }





    @Override
    public Set<String> getSupportedProtocols()
    {
        HashSet<String> hsProtocols = new HashSet<String>();

        hsProtocols.add("imap");
        hsProtocols.add("imaps");

        return hsProtocols;
    }

}
