/*
 * Copyright (c) 2005 - 2008 Aduna.
 * All rights reserved.
 * 
 * Licensed under the Aperture BSD-style license.
 */
package de.dfki.km.leech.util.certificates;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A CertificateStore that holds the root Certificates. As this set is not mutable by the application
 * itself, this class only provides functionality to load the certificates from a file.
 */
public class RootCertificateStore implements CertificateStore {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String fileName;

    private KeyStore keyStore;

    public RootCertificateStore(String fileName) {
        this.fileName = fileName;
    }

    public void load() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // initialize a KeyStore containing the root certificates
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction() {

                public Object run() throws KeyStoreException, NoSuchAlgorithmException, CertificateException,
                        IOException {
                    // initialize the KeyStore instance
                    if (keyStore == null) {
                        keyStore = KeyStore.getInstance("JKS");
                        keyStore.load(null, null);
                    }

                    // load the certificates, if they exist
                    File certFile = new File(fileName);
                    if (certFile.exists()) {
                        FileInputStream stream = new FileInputStream(certFile);
                        BufferedInputStream buffer = new BufferedInputStream(stream);

                        keyStore.load(buffer, null);

                        buffer.close();
                        stream.close();
                    }

                    return null;
                }
            });
        }
        catch (PrivilegedActionException e) {
            Exception ex = e.getException();

            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            else if (ex instanceof CertificateException) {
                throw (CertificateException) ex;
            }
            else if (ex instanceof KeyStoreException) {
                throw (KeyStoreException) ex;
            }
            else if (ex instanceof NoSuchAlgorithmException) {
                throw (NoSuchAlgorithmException) ex;
            }
            else {
                logger.error("Unrecognized nested exception, ignoring", e);
            }
        }
    }

    public void save() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // no-op
    }

    public void add(Certificate certificate) throws KeyStoreException {
        // no-op
    }

    public void remove(Certificate certificate) throws KeyStoreException {
        // no-op
    }

    public boolean contains(Certificate certificate) throws KeyStoreException {
        // no-op
        return false;
    }

    public boolean verify(Certificate certificate) throws KeyStoreException {
        Enumeration aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = (String) aliases.nextElement();
            Certificate rootCertificate = keyStore.getCertificate(alias);

            try {
                certificate.verify(rootCertificate.getPublicKey());
                return true;
            }
            catch (Exception e) {
                // verification failed, ignore exception (is part of normal operation)
            }
        }

        return false;
    }

    public Iterator iterator() throws KeyStoreException {
        HashSet set = new HashSet();
        Enumeration aliases = keyStore.aliases();

        while (aliases.hasMoreElements()) {
            String alias = (String) aliases.nextElement();
            set.add(keyStore.getCertificate(alias));
        }

        return set.iterator();
    }
}
