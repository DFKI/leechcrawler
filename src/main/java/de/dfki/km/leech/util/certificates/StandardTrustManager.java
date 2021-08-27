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



import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;

/**
 * A default implementation, well-suited for most environments, of X509TrustManager. It handles all
 * certificates that can be validated by the system certificates and uses a delegate mechanism to decide
 * what to do with unknown certificates. Such a delegate may for example show a dialog asking the user
 * what to do, similar to what web browsers and mail readers typically do.
 */
public class StandardTrustManager implements X509TrustManager {

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * The file name of the standard root certificates file.
     */
    private static final String ROOT_CERTIFICATES_FILE_NAME;

    /**
     * The file name of the JSSE root certificates file.
     */
    private static final String JSSE_ROOT_CERTIFICATES_FILE_NAME;

    static {
        // determine where the root certificates and HTTPS root certificates are stored
        String securityPath = System.getProperty("java.home") + File.separator + "lib" + File.separator
                + "security" + File.separator;
        ROOT_CERTIFICATES_FILE_NAME = securityPath + "cacerts";
        JSSE_ROOT_CERTIFICATES_FILE_NAME = securityPath + "jssecacerts";
    }

    /**
     * The CertificateStore that holds all Certificates approved for this session.
     */
    private CertificateStore sessionCertificateStore;

    /**
     * The CertificateStore that holds all Certificates denied for this session.
     */
    private CertificateStore deniedCertificateStore;

    /**
     * The CertificateStore that holds all standard root certificates.
     */
    private CertificateStore rootCertificateStore;

    /**
     * The CertificateStore that holds all JSSE root certificates.
     */
    private CertificateStore jsseRootCertificateStore;

    /**
     * The CertificateStore that holds all permanently approved certificates.
     */
    private CertificateStore persistentCertificateStore;

    /**
     * A system-provided TrustManager used to verify a chain of certificates before checking the chain
     * against our own CertificateStores. This is only overruled by the CertificateStores holding the
     * denied certificates.
     */
    private X509TrustManager defaultTrustManager;

    /**
     * The delegate used to decide on how to judge a Certificate when it cannot be verified using the
     * default trust manager or one of the CertificateStores.
     */
    private TrustDecider trustDecider;

    /**
     * Create a StandardTrustManager that has no persistent storage for permanently approved
     * certificates.
     */
    public StandardTrustManager() throws CertificateException, KeyStoreException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {
        this(null, null);
    }

    /**
     * Create a StandardTrustManager that uses the specified File to store its permanently approved
     * certificates.
     * 
     * @param pcsFile A File to load and store the certificates, or null when no certificates should be
     *            loaded and stored.
     * @param pcsPassword The password used to check the integrity of the keystore, the password used to
     *            unlock the keystore, or null.
     */
    public StandardTrustManager(File pcsFile, char[] pcsPassword) throws CertificateException,
            KeyStoreException, IOException, NoSuchAlgorithmException, NoSuchProviderException {
        // create the certificate stores
        sessionCertificateStore = new SessionCertificateStore();
        deniedCertificateStore = new SessionCertificateStore();
        rootCertificateStore = new RootCertificateStore(ROOT_CERTIFICATES_FILE_NAME);
        jsseRootCertificateStore = new RootCertificateStore(JSSE_ROOT_CERTIFICATES_FILE_NAME);
        if (pcsFile == null) {
            persistentCertificateStore = new SessionCertificateStore();
        }
        else {
            persistentCertificateStore = new PersistentCertificateStore(pcsFile, pcsPassword);
        }

        // let all CertificateStores load their Certificates (some may choose to ignore this operation)
        sessionCertificateStore.load();
        deniedCertificateStore.load();
        rootCertificateStore.load();
        jsseRootCertificateStore.load();
        persistentCertificateStore.load();

        // create an instance of the default TrustManager
        TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
        factory.init((KeyStore) null);
        TrustManager[] managers = factory.getTrustManagers();
        defaultTrustManager = (X509TrustManager) managers[0];
    }

    public void setTrustDecider(TrustDecider trustDecider) {
        this.trustDecider = trustDecider;
    }

    public TrustDecider getTrustDecider() {
        return trustDecider;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkChain(chain, authType, true);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        checkChain(chain, authType, false);
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }

    private synchronized void checkChain(X509Certificate[] chain, String authType, boolean checkClient)
            throws CertificateException {
        boolean rootCANotValid = false;
        boolean timeNotValid = false;

        Decision decision = null;

        try {
            // check if the certificate has been denied before (overrules the default trust manager)
            if (deniedCertificateStore.contains(chain[0])) {
                throw new CertificateException("certificate has been denied");
            }

            // let the default trust manager inspect the certificates
            try {
                if (checkClient) {
                    defaultTrustManager.checkClientTrusted(chain, authType);
                }
                else {
                    defaultTrustManager.checkServerTrusted(chain, authType);
                }

                // no exceptions while checking the certificates, so they are ok
                return;
            }
            catch (CertificateException e) {
                // certificate verification failed, proceed
            }

            // check if the certificate has been accepted for this session before
            if (sessionCertificateStore.contains(chain[0])) {
                return;
            }

            // check if the certificate has been permanently accepted before
            if (persistentCertificateStore.contains(chain[0])) {
                return;
            }

            // loop through all the certs in chain
            for (int i = 0; i < chain.length; i++) {
                X509Certificate cert = chain[i];

                // check if one of the certificates in the chain could not be verified
                if (!jsseRootCertificateStore.verify(cert) && !rootCertificateStore.verify(cert))
                    rootCANotValid = true;

                // Check if the cert has expired.
                try {
                    cert.checkValidity();
                }
                catch (CertificateExpiredException e) {
                    timeNotValid = true;
                }
                catch (CertificateNotYetValidException e) {
                    timeNotValid = true;
                }
            }

            // let the TrustDecider decise
            TrustDecider trustDecider = getTrustDecider();
            if (trustDecider == null) {
                throw new CertificateException("trust could not be established");
            }
            else {
                decision = trustDecider.decide(chain, rootCANotValid, timeNotValid);
                X509Certificate cert = chain[0];

                if (Decision.TRUST_THIS_SESSION.equals(decision)) {
                    sessionCertificateStore.add(cert);
                    sessionCertificateStore.save();
                }
                else if (Decision.TRUST_ALWAYS.equals(decision)) {
                    persistentCertificateStore.add(cert);
                    persistentCertificateStore.save();
                }
                else {
                    deniedCertificateStore.add(cert);
                    deniedCertificateStore.save();
                }
            }
        }
        catch (CertificateException e) {
            throw e;
        }
        catch (Throwable e) {
            // a new exception is thrown below, no need to throw one here
            logger.error("Unexpected throwable while verifying certificate", e);
        }

        if (!Decision.TRUST_THIS_SESSION.equals(decision) && !Decision.TRUST_ALWAYS.equals(decision)) {
            throw new CertificateException("trust manager could not trust certificate chain");
        }
    }
}
