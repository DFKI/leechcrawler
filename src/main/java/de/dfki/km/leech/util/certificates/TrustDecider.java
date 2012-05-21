/*
 * Copyright (c) 2005 - 2008 Aduna and Deutsches Forschungszentrum fuer Kuenstliche Intelligenz DFKI GmbH.
 * All rights reserved.
 * 
 * Licensed under the Aperture BSD-style license.
 */
package de.dfki.km.leech.util.certificates;

import java.security.cert.X509Certificate;

/**
 * A TrustDecider is typically used by a StandardTrustManager to implement a strategy for deciding on
 * Certificates that cannot be automatically verified. A TrustDecider implementation may for example open
 * a dialog asking the user what to do with the presented dialog.
 */
public interface TrustDecider {

    /**
     * Lets the TrustDecider decide on the specified Certificate chain.
     * 
     * @param chain The chain of X509Certificates to decide on.
     * @param rootCANotValid Flag that indicates whether one of the root certificates could not be
     *            verified by one of the system certificates.
     * @param timeNotValid Flag to indicate whether one of the root certificates is not valid yet or has
     *            expired.
     * @return A Decision instance indicating the decision made by this TrustDecider.
     */
    public Decision decide(X509Certificate[] chain, boolean rootCANotValid, boolean timeNotValid);
}
