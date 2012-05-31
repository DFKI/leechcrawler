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
