package executors;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;
import utils.Sha1;
import utils.ZlibDecompress;

public class HashObject implements Executor {
  private final String[] params;

  public HashObject(String... params) {
    this.params = params;
  }

  @Override
  public void execute() {
    if (params.length < 3) {
      throw new IllegalArgumentException("Few command line arguments for `cat-file`");
    }
    final String readPath = params[2];
    try {
      byte[] contentBytes = Files.readAllBytes(Path.of(readPath));
      byte size = (byte) contentBytes.length;
      byte[] headerBytes = ("blob " + size + '\0').getBytes(StandardCharsets.UTF_8);
      byte[] blobBytes = new byte[headerBytes.length + contentBytes.length];
      System.arraycopy(headerBytes, 0, blobBytes, 0, headerBytes.length);
      System.arraycopy(contentBytes, 0, blobBytes, headerBytes.length, contentBytes.length);

      String hash = Sha1.hash(blobBytes);
      String objectDir = hash.substring(0, 2);
      String objectFile = hash.substring(2);
      String writePath = ".git/objects/" + objectDir + "/" + objectFile;
      byte[] compressedContent = ZlibDecompress.compress(blobBytes);

      Path path = Paths.get(writePath);
      Path parentDir = path.getParent();
      if (parentDir != null && !Files.exists(parentDir)) {
        Files.createDirectories(parentDir);
      }
      Files.createFile(path);
      Files.write(path, compressedContent);
      System.out.println(hash);
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
