package client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class GitClient {
  private final String repoUrl;

  public GitClient(String repoUrl) {
    this.repoUrl = repoUrl;
  }

  public String remoteRefAdvertisement() throws IOException {
    String refAdvertisementUrl = "/info/refs?service=git-upload-pack";
    try (HttpClient client = HttpClient.newHttpClient()) {
      HttpRequest request = HttpRequest
          .newBuilder()
          .GET()
          .uri(URI.create(repoUrl + refAdvertisementUrl))
          .header("Accept", "application/x-git-upload-pack-advertisement")
          .build();
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        return response.body();
      }
      throw new IOException("Git fetch failed");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
