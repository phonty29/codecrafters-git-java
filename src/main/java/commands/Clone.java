package commands;


import proto.GitClient;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.DataFormatException;
import struct.Mode;
import struct.ObjectType;
import struct.TreeObject;
import utils.Git;
import utils.Pkt;
import utils.Zlib;

public class Clone {

  private final String[] args;
  private Path absolutePath;

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
      this.absolutePath = Files.createDirectory(Paths.get(gitDir));
      new Init(this.absolutePath).execute();
      var gitClient = new GitClient(repoUrl);
      String refAdvertisement = gitClient.getRemoteRefs();
      String headSha = Pkt.retrieveHeadShaFromPktFormattedRefs(refAdvertisement);
      byte[] negotiationPayload = Pkt.createPktNegotiationPayload(headSha);
      ByteBuffer packBuffer = gitClient.getPackFile(negotiationPayload);
      byte[] firstCommitHash = Git.processPackFile(absolutePath, packBuffer);
      byte[] firstCommitContent = Git.readGitObject(this.absolutePath, firstCommitHash);
      String treeHash = new String(firstCommitContent).split(ObjectType.TREE + " ")[1].split("\n")[0];
      checkoutTree(this.absolutePath, treeHash);
      System.out.println("Cloned repository to " + gitDir);
    } catch (IOException e) {
      System.err.println("Error: " + e.getMessage() + " " + e.getClass().getName());
    }
  }

  private void checkoutTree(Path targetDirectory, String treeHash) throws IOException {
    TreeObject[] treeObjects = getTreeObjects(treeHash);
    for (TreeObject entry : treeObjects) {
      Path targetPath = targetDirectory.resolve(entry.name());
      if (Mode.DIRECTORY.equals(entry.mode())) {
        checkoutTree(targetPath, entry.hash());
      } else if (Mode.isBlob(entry.mode())) {
        byte[] blobHash = HexFormat.of().parseHex(entry.hash());
        byte[] gitObject = Git.readGitObject(this.absolutePath, blobHash);
        byte[] content = Git.removeGitHeader(gitObject);
        Files.createDirectories(targetDirectory);
        Files.write(targetPath, content);
      }
    }
  }

  private TreeObject[] getTreeObjects(String treeSha) {
    String objectDir = treeSha.substring(0, 2);
    String objectFile = treeSha.substring(2);
    Path path = this.absolutePath.resolve(".git/objects/" + objectDir + "/" + objectFile);
    try {
      byte[] bytes = Files.readAllBytes(path);
      byte[] decompressed = Zlib.decompressObject(bytes);
      byte[] data = Git.removeGitHeader(decompressed);
      List<TreeObject> result = new ArrayList<>();
      int pos = 0;

      while (pos < data.length) {
        int space = findByte(data, pos, (byte) ' ');
        String modeStr = new String(data, pos, space - pos);
        Mode mode = Mode.fromNumber(modeStr);
        pos = space + 1;
        int nullByte = findByte(data, pos, (byte) 0);
        String fileName = new String(data, pos, nullByte - pos);
        pos = nullByte + 1;
        byte[] shaBytes = Arrays.copyOfRange(data, pos, pos + 20);
        String sha = HexFormat.of().formatHex(shaBytes);
        pos += 20;
        result.add(new TreeObject(mode, fileName, sha));
      }
      return result.toArray(new TreeObject[0]);
    } catch (IOException | DataFormatException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  private int findByte(byte[] data, int start, byte target) {
    for (int i = start; i < data.length; i++) {
      if (data[i] == target) {
        return i;
      }
    }
    throw new IllegalStateException("Byte not found");
  }
}
