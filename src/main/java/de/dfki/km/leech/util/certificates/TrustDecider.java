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
