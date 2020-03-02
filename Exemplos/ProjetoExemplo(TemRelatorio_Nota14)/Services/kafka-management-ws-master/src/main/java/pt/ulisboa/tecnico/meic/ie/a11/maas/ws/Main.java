package pt.ulisboa.tecnico.meic.ie.a11.maas.ws;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import javax.xml.ws.Endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Properties props = new Properties();
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static String hostAddress;

  public static void main(String[] args) {
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();

    try (final InputStream is = loader.getResourceAsStream("app.properties")) {
      props.load(Objects.requireNonNull(is));
    } catch (IOException | NullPointerException e) {
      logger.error(e.getMessage(), e);
      return;
    }

    // in case we're on AWS
    final String host = getHostAddress().orElse(props.getProperty("service.host", "localhost"));
    final var port = Short.parseShort(props.getProperty("service.port", "9997"));
    final var path = props.getProperty("service.path", "/kafkamgmt");

    final var uriBuilder = new StringBuilder("http://").append(host);
    if (port != 80 && port != 443) {
      uriBuilder.append(":").append(port);
    }
    uriBuilder.append(path);

    final Endpoint endpoint = Endpoint.create(new KafkaManager());
    endpoint.publish(uriBuilder.toString());
    logger.info("Endpoint published: {}", uriBuilder.toString());
  }

  static Optional<String> getHostAddress() {
    if (hostAddress != null) {
      return Optional.of(hostAddress);
    }

    final var req = HttpRequest.newBuilder()
        .uri(URI.create("http://169.254.169.254/latest/meta-data/local-ipv4/"))
        .build();

    try {
      final HttpResponse<String> res = HttpClient.newHttpClient()
          .send(req, BodyHandlers.ofString());
      if (res.statusCode() == HttpURLConnection.HTTP_OK) {
        hostAddress = res.body();
        return Optional.of(hostAddress);
      }
    } catch (IOException | InterruptedException ignored) {
    }

    return Optional.empty();
  }
}
