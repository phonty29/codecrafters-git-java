package executors;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import client.GitClient;
import utils.GitUtils;
import utils.PktUtils;

public class Clone {
  private final String[] args;

  public Clone(String... args) {
    this.args = args;
  }

  public void execute() {
    if (args.length < 3) {
      throw new IllegalArgumentException("Too few arguments in `clone` command");
    }

    String repoUrl = args[1];
    String gitDir = args[2];
    try {
      Path absolutePath = Files.createDirectory(Paths.get(gitDir));
      new Init(absolutePath).execute();
      var gitClient = new GitClient(repoUrl);
      String refAdvertisement = gitClient.getRemoteRefs();
      String headSha = PktUtils.retrieveHeadShaFromPktFormattedRefs(refAdvertisement);
      byte[] negotiationPayload = PktUtils.createPktNegotiationPayload(headSha);
      ByteBuffer packBuffer = gitClient.getPackFile(negotiationPayload);
      GitUtils.processPackFile(packBuffer.asReadOnlyBuffer());
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
