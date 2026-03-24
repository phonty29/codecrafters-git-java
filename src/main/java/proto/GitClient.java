package proto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.ByteBuffer;

public class GitClient {

  private final String repoUrl;

  public GitClient(String repoUrl) {
    this.repoUrl = repoUrl;
  }

  public String getRemoteRefs() throws IOException {
    try (HttpClient client = HttpClient.newHttpClient()) {
      String ENDPOINT = "/info/refs?service=git-upload-pack";
      HttpRequest request = HttpRequest
          .newBuilder()
          .GET()
          .uri(URI.create(repoUrl + ENDPOINT))
          .header("Accept", "application/x-git-upload-pack-advertisement")
          .build();
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      if (response.statusCode() == 200) {
        return response.body();
      }
      throw new IOException("Git remote refs fetch failed");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public ByteBuffer getPackFile(byte[] negotiationPayload) throws IOException {
    try (HttpClient client = HttpClient.newHttpClient()) {
      String ENDPOINT = "/git-upload-pack";
      HttpRequest request = HttpRequest
          .newBuilder()
          .POST(HttpRequest.BodyPublishers.ofByteArray(negotiationPayload))
          .uri(URI.create(repoUrl + ENDPOINT))
          .header("Content-Type", "application/x-git-upload-pack-request")
          .build();
      HttpResponse<byte[]> response = client.send(request, BodyHandlers.ofByteArray());
      if (response.statusCode() == 200) {
        return ByteBuffer.wrap(response.body());
      }
      throw new IOException("Git packfile fetch failed");
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
