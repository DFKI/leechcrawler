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

package de.dfki.km.leech.util;

public class LeechException extends RuntimeException
{
    private static final long serialVersionUID = 7184624329588842248L;

    public LeechException()
    {
        super();
    }

    public LeechException(String message)
    {
        super(message);
    }



    public LeechException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public LeechException(Throwable cause)
    {
        super(cause);
    }

}
