package client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;

public class GitClient {
  private final String repoUrl;

  public GitClient(String repoUrl) {
    this.repoUrl = repoUrl;
  }

  public String getRemoteRefs() throws IOException {
    try (HttpClient client = HttpClient.newHttpClient()) {
      String REF_ADVERTISEMENT_URL = "/info/refs?service=git-upload-pack";
      HttpRequest request = HttpRequest
          .newBuilder()
          .GET()
          .uri(URI.create(repoUrl + REF_ADVERTISEMENT_URL))
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
