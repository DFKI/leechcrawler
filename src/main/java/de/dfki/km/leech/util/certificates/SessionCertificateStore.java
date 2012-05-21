/*
 * Copyright (c) 2005 - 2008 Aduna.
 * All rights reserved.
 * 
 * Licensed under the Aperture BSD-style license.
 */
package de.dfki.km.leech.util.certificates;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A CertificateStore containing those Certificates that only need to be registered for this session.
 * Typically used to hold all temporarily accepted or rejected Certificates.
 */
public class SessionCertificateStore implements CertificateStore {

    private HashSet certificates;

    public SessionCertificateStore() {
        certificates = new HashSet();
    }

    public void load() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // no-op
    }

    public void save() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        // no-op
    }

    public void add(Certificate certificate) throws KeyStoreException {
        certificates.add(certificate);
    }

    public void remove(Certificate certificate) throws KeyStoreException {
        certificates.remove(certificate);
    }

    public boolean contains(Certificate certificate) throws KeyStoreException {
        return certificates.contains(certificate);
    }

    public boolean verify(Certificate cert) throws KeyStoreException {
        // we don't verify certificates, we only contain them
        return false;
    }

    public Iterator iterator() throws KeyStoreException {
        return certificates.iterator();
    }
}
