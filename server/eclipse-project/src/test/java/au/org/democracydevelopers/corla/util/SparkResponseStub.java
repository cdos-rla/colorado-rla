/*
Democracy Developers IRV extensions to colorado-rla.

@copyright 2024 Colorado Department of State

These IRV extensions are designed to connect to a running instance of the raire
service (https://github.com/DemocracyDevelopers/raire-service), in order to
generate assertions that can be audited using colorado-rla.

The colorado-rla IRV extensions are free software: you can redistribute it and/or modify it under the terms
of the GNU Affero General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

The colorado-rla IRV extensions are distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with
raire-service. If not, see <https://www.gnu.org/licenses/>.
*/

package au.org.democracydevelopers.corla.util;

import spark.Response;

import javax.servlet.ServletOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that can behave like a Spark request, for testing endpoints.
 * Note this does _not_ (yet) implement all the functions you might need - it contains only the ones
 * that I noticed being used in colorado-rla.
 * There are others, including redirect(), type() and raw() that are not yet implemented.
 * See <a href="https://javadoc.io/doc/com.sparkjava/spark-core/2.5.4/spark/Response.html">...</a> for types.
 * In particular, raw() causes problems for at least some endpoints.
 */
public class SparkResponseStub extends Response {
    private String _body;
    private int _statusCode;
    private Map<String,String> headers = new HashMap<>();
    private ServletOutputStream _os;

    public SparkResponseStub() {
        super();
    }

    /**
     * Set the body.
     * @param body the body to be set.
     */
    public void body(String body) {
        _body = body;
    }

    /**
     * Get the body.
     */
    public String body() {
        return _body;
    }

    /**
     * Set the http status code.
     * @param statusCode the http status code.
     */
    public void status(int statusCode) {
        _statusCode = statusCode;
    }

    /**
     * Set the header - add <name,value> to the headers map.
     * @param name  the name of the header.
     * @param value the value for it to be set to.
     */
    public void header(final String name, final String value) {
        headers.put(name, value);
    }

    /**
     * Get the http status.
     * @return the status.
     */
    public int status() {
        return _statusCode;
    }
}
