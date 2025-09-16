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

package au.org.democracydevelopers.corla.endpoint;

import au.org.democracydevelopers.corla.model.ContestType;
import com.google.gson.Gson;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.Main;
import us.freeandfair.corla.asm.ASMEvent;
import us.freeandfair.corla.endpoint.AbstractDoSDashboardEndpoint;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.BallotManifestInfoQueries;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static us.freeandfair.corla.controller.ContestCounter.countContest;

/**
 * An abstract endpoint for communicating with raire. Includes all the information for collecting IRV contests
 * and making a request to raire, including the location of the raire service.
 * Used by GetAssertions and GenerateAssertions.
 */
public abstract class AbstractAllIrvEndpoint extends AbstractDoSDashboardEndpoint {

    /**
     * Class-wide logger
     */
    private static final Logger LOGGER = LogManager.getLogger(AbstractAllIrvEndpoint.class);

    /**
     * GSON, for serialising requests.
     */
    protected final static Gson gson = new Gson();

    /**
     * Identify RAIRE service URL from config.
     */
    protected static final String RAIRE_URL = "raire_url";

    /**
     * The httpClient used for making requests to the raire-service.
     */
    protected final static HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * The event to return for this endpoint.
     */
    protected final ThreadLocal<ASMEvent> my_event = new ThreadLocal<>();

    /**
     * @return State admin authorization is necessary for this endpoint.
     */
    public AuthorizationType requiredAuthorization() { return AuthorizationType.STATE; }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ASMEvent endpointEvent() {
        return my_event.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        my_event.set(null);
    }

    /**
     * Get (or make) all the ContestResults whose contests are consistently IRV.
     * Used for assertion generation and retrieval.
     * Uses manifests if they are there, but just counts the CSVs if not.
     * This is analogous to (and mostly copied from) ContestCounter::countAllContests, but restricted
     * to the IRV ones. Although the countContest function isn't really useful or meaningful for IRV,
     * it is called here because it actually does a lot of other useful things, such as setting the
     * number of allowed winners and gathering all the results across counties.
     * Assumption: Contest names are unique.
     * @return A list of all ContestResults for IRV contests.
     * @throws RuntimeException if it encounters contests with a mix of IRV and any other contest type.
     */
    protected static List<ContestResult> getIRVContestResults() {
        final String prefix = "[getIRVContestResults]";
        final String msg = "Inconsistent contest types:";


        List<ContestResult> results = Persistence.getAll(CountyContestResult.class)
            .stream()
            // Collect contests by name across counties.
            .collect(Collectors.groupingBy(x -> x.contest().name()))
            .entrySet()
            .stream()
            .filter(
               // Filter for those with any IRV descriptions (which should be all)
               ((Map.Entry<String, List<CountyContestResult>> countyContestResults) ->
                countyContestResults.getValue().stream().map(ccr -> ccr.contest().description())
                    .anyMatch(d -> d.equalsIgnoreCase(ContestType.IRV.toString())))
            )
            // 'Count' them (which actually does plurality counting and sets various useful values
            // such as number of winners).
            .map((Map.Entry<String, List<CountyContestResult>> countyContestResults) ->  {
                // Use manifests (for the denominator of the diluted margin) if _all_ counties have
                // uploaded one.
                boolean useManifests = countyContestResults.getValue().stream().
                    allMatch(ccr -> BallotManifestInfoQueries.totalBallots(Set.of(ccr.county().id())) > 0);
                return countContest(countyContestResults, useManifests);
                }
            )
            .toList();

        // The above should be sufficient, but just in case, check that each contest we found _all_
        // matches IRV, and throw a RuntimeException if not - one contest must not mix plurality and
        // IRV.
        for (final ContestResult cr : results) {
            if (cr.getContests().stream().map(Contest::description)
                .anyMatch(d -> !d.equalsIgnoreCase(ContestType.IRV.toString()))) {
                LOGGER.error(String.format("%s %s %s", prefix, msg, cr.getContestName()));
                throw new RuntimeException(msg + cr.getContestName());
            }
        }

        return results;
    }

    /**
     * Ping the raire service /hello endpoint.
     * This is just an easy way of checking that the service is reachable.
     * @return "OK" if the server is reachable, "Unreachable" otherwise.
     */
    public static String pingRaireService() {
        final String prefix = "[pingRaireService]";

        // The response string to return when the service is unreachable.
        final String unreachable = "Unreachable";

        // The response to return when the raire service is reachable.
        final String ok = "OK";

        final String helloUrl
            = Main.properties().getProperty(RAIRE_URL, "") + "/raire/hello";

        try {
            final HttpResponse<String> raireResponse = httpClient.send(HttpRequest.newBuilder()
                    .uri(new URL(helloUrl).toURI())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            LOGGER.debug(String.format("%s %s.", prefix, "Pinged Raire service"));

            final int statusCode = raireResponse.statusCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {

                // The raire service is OK.
                LOGGER.debug(String.format("%s %s.", prefix, "OK response received from RAIRE"));
                return ok;

            } else {

                // The raire service is not OK. This also happens if the port or url are wrong.
                LOGGER.warn(String.format("%s Error %s %s.", prefix, statusCode, "received from RAIRE"));
                return unreachable;

            }
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            // This happens if the url specified in default.properties is an invalid/unparseable url.
            final String msg = "Bad configuration of raire-service url. Fix the config file.";
            LOGGER.error(String.format("%s %s %s", prefix, msg, e.getMessage()));
        } catch (IOException | InterruptedException e) {
            final String msg = "Failed attempt to ping raire service";
            LOGGER.warn(String.format("%s %s.", prefix, msg));
        }
        return unreachable;
    }
}
