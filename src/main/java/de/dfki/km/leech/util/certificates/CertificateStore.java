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
import java.util.Iterator;

/**
 * The base interface for all certificate stores used by the StandardTrustManager.
 */
public interface CertificateStore {

    /**
     * Loads all certificates belonging to this store (optional operation).
     */
    public void load() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException;

    /**
     * Saves all certificates belonging to this store (optional operation).
     */
    public void save() throws IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException;

    /**
     * Add a Certificate to this CertificateStore (optional operation).
     * 
     * @param certificate The Certificate to add.
     */
    public void add(Certificate certificate) throws KeyStoreException;

    /**
     * Remove a Certificate from this CertificateStore (optional operation);
     * 
     * @param certificate The Certificate to remove.
     */
    public void remove(Certificate certificate) throws KeyStoreException;

    /**
     * Returns whether this certificate store contains the specified Certificate (optional operation).
     * 
     * @param certificate The Certificate that is tested for presence in this CertificateStore.
     * @return 'true' when this CertificateStore contains the specified Certificate, 'false' otherwise.
     */
    public boolean contains(Certificate certificate) throws KeyStoreException;

    /**
     * Verifies the supplied Certificate against the certificates in this store (optional operation).
     * 
     * @param certificate The Certificate to verify.
     * @return 'true' when the Certificate could successfully be verified, 'false' otherwise.
     */
    public boolean verify(Certificate certificate) throws KeyStoreException;

    /**
     * Returns an Iterator that iterates over all Certificates in this store.
     * 
     * @return An Iterator returning Certificate instances.
     */
    public Iterator iterator() throws KeyStoreException;
}
