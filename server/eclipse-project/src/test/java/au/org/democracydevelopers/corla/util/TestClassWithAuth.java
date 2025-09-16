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

import org.mockito.Mock;
import spark.Response;
import us.freeandfair.corla.auth.AuthenticationInterface;
import us.freeandfair.corla.endpoint.Endpoint;
import us.freeandfair.corla.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static us.freeandfair.corla.model.Administrator.AdministratorType.COUNTY;
import static us.freeandfair.corla.model.Administrator.AdministratorType.STATE;

/**
 * This test class is designed for testing endpoints. It mocks successful authentication to allow
 * for direct calling of endpointBody(request, response).
 * Some notes:
 * - It is highly dependent on the mocked Spark functions in SparkRequestStub and SparkResponseStub.
 *   Not all the Spark functions have been implemented - if you find that the attempt to use one of
 *   them fails, you need to implement it in either SparkRequestStub or SparkResponseStub.
 * - You will probably also still need to do some other set up of the endpoint you want to test.
 * - Tests run in parallel seem to fail in strange ways - either do them sequentially, or make them
 * one test. I haven't tried testing parallel runs of mocked auth for _different_ counties.
 * See ACVRUploadTests for an example. Calling endpoint.before(request, response) was necessary for
 * that endpoint.
 */
public abstract class TestClassWithAuth extends TestClassWithDatabase {

  /**
   * A blank response for use in endpoint-calling.
   */
  protected final Response response = new SparkResponseStub();

  @Mock
  protected AuthenticationInterface auth;

  @Mock
  protected ThreadLocal<List<LogEntry>> mockedAsm;

  /**
   * Init mocked objects, particularly the authentication as the given county.
   * The main thing to mock here is the outcome of AbstractEndpoint::checkAuthorization.
   * @param name The name of the county - must match whatever is loaded into the test database.
   * @param ID   The ID of the county or state admin - must match whatever is loaded into the test database.
   * For example, to mock Auth as Adams County (ID = 1), use mockAuth("Adams", 1L, COUNTY);
   */
  protected void mockAuth(final String name, final long ID,
                          final Endpoint.AuthorizationType authType) {

    // Mock successful auth as a county. No need to mock the CountyDashboard retrieval from
    // the database, because that is loaded in via co-counties.sql.
    when(auth.secondFactorAuthenticated(any())).thenReturn(true);
    // You can change the second "any()" to STATE or COUNTY if you want the other _not_ to be authenticated.
    when(auth.authenticatedAs(any(), any(), any())).thenReturn(true);
    when(mockedAsm.get()).thenReturn(new ArrayList<>());

    // Mock the authenticated administrator. The type and username seem to be the only things that
    // actually get checked.
    if (authType == Endpoint.AuthorizationType.COUNTY) {
      // Auth as the requested County.
      final County county = new County(name, ID);
      when(auth.authenticatedAdministrator(any())).thenReturn(
          new Administrator("countyadmin" + ID, COUNTY, name + " County", county));
      when(auth.authenticatedCounty(any())).thenReturn(county);

      // Auth as a State admin. This covers STATE, EITHER and NONE.
    } else {
      when(auth.authenticatedAdministrator(any())).thenReturn(
          new Administrator("stateadmin" + ID, STATE, name + "State admin", null));

      when(auth.authenticatedCounty(any())).thenReturn(null);
    }
  }
}