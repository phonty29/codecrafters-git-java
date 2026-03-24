package commands;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.DataFormatException;
import proto.GitClient;
import struct.Mode;
import struct.ObjectType;
import struct.TreeObject;
import git.Git;
import utils.Pkt;

public class Clone {

  private final GitClient gitClient;
  private final Git git;

  public Clone(String... args) {
    if (args.length < 3) {
      throw new IllegalArgumentException("Too few arguments in `clone` command");
    }
    String repoUrl = args[1];
    String gitDir = args[2];
    this.gitClient = new GitClient(repoUrl);
    this.git = new Git(Path.of(gitDir));
  }

  public void execute() {
    try {
      new Init(git.getRoot()).execute();
      String refAdvertisement = gitClient.fetchRefs();
      String headSha = Pkt.retrieveHeadShaFromPktFormattedRefs(refAdvertisement);
      byte[] negotiationPayload = Pkt.createPktNegotiationPayload(headSha);
      ByteBuffer packBuffer = gitClient.fetchPackfile(negotiationPayload);
      byte[] firstCommitHash = git.processPackFile(packBuffer);
      byte[] firstCommitContent = git.readGitObject(firstCommitHash);
      String treeHash = new String(firstCommitContent).split(ObjectType.TREE + " ")[1].split("\n")[0];
      checkoutTree(git.getRoot(), treeHash);
      System.out.println("Cloned repository to " + git.getRoot());
    } catch (IOException | DataFormatException e) {
      System.err.println("Error: " + e.getMessage());
    }
  }

  private void checkoutTree(Path targetDirectory, String treeHash) throws IOException, DataFormatException {
    List<TreeObject> treeObjects = git.readTree(treeHash);
    for (TreeObject entry : treeObjects) {
      Path targetPath = targetDirectory.resolve(entry.name());
      if (Mode.DIRECTORY.equals(entry.mode())) {
        checkoutTree(targetPath, entry.hash());
      } else if (Mode.isBlob(entry.mode())) {
        byte[] blobHash = HexFormat.of().parseHex(entry.hash());
        byte[] gitObject = git.readGitObject(blobHash);
        byte[] content = Git.removeGitHeader(gitObject);
        Files.createDirectories(targetDirectory);
        Files.write(targetPath, content);
      }
    }
  }
}
