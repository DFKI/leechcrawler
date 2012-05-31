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

/**
 * Instances of this class are used to model a decision taken by a TrustDecider.
 * 
 * @see de.dfki.km.leech.util.certificates.semanticdesktop.aperture.security.trustdecider.TrustDecider
 */
public class Decision {

    public static final Decision TRUST_THIS_SESSION = new Decision("trust for this session");

    public static final Decision TRUST_ALWAYS = new Decision("trust always");

    public static final Decision DISTRUST = new Decision("never trust");

    private String name;

    private Decision(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Decision) {
            Decision other = (Decision) object;
            return name.equals(other.getName());
        }
        else {
            return false;
        }
    }
}
