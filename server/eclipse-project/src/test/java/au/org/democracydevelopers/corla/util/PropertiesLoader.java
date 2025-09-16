package au.org.democracydevelopers.corla.util;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads a properties file. Used for test.properties.
 * Copied from <a href="https://www.baeldung.com/inject-properties-value-non-spring-class">...</a>
 */
public class PropertiesLoader {

  public static final String TEST_PROPERTIES_FILE = "test.properties";

  private static final Logger LOGGER = LogManager.getLogger(PropertiesLoader.class);

    public static Properties loadProperties() {
      final String prefix = "loadProperties";

      Properties configuration = new Properties();
      InputStream inputStream = PropertiesLoader.class
          .getClassLoader()
          .getResourceAsStream(TEST_PROPERTIES_FILE);
      try {
        configuration.load(inputStream);
        inputStream.close();
      } catch (NullPointerException | IOException e) {
        LOGGER.error(String.format("%s %s", prefix, "Could not load test properties. Check that a file called "
            + TEST_PROPERTIES_FILE + "exists in the test directory"));
        throw new RuntimeException(e);
      }
      return configuration;
    }
}
