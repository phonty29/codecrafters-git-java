package executors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.DataFormatException;
import struct.Mode;
import struct.TreeObject;
import utils.ByteUtils;
import utils.Zlib;

public class LsTree implements Executor {

  private final String[] params;

  public LsTree(String... params) {
    this.params = params;
  }

  @Override
  public void execute() {
    if (params.length < 2) {
      throw new IllegalArgumentException("Few command line arguments for `ls-tree`");
    }
    final String treeSha1 = params[2];
    if (treeSha1.length() != 40) {
      throw new IllegalArgumentException("Hash length must be 40 characters");
    }

    String objectDir = treeSha1.substring(0, 2);
    String objectFile = treeSha1.substring(2);
    String path = ".git/objects/" + objectDir + "/" + objectFile;
    try {
      byte[] bytes = Files.readAllBytes(Path.of(path));
      byte[] decompressed = Zlib.decompress(bytes);
      List<byte[]> split = ByteUtils.split(decompressed, (byte) 0);
      TreeObject[] objects = new TreeObject[split.size() - 2];
      for (int i = 1; i < split.size() - 1; i++) {
        String[] splitContent = new String(split.get(i)).split(" ");
        Mode mode = Mode.fromNumber(splitContent[0]);
        String fileName = splitContent[1];
        byte[] hashBytes = ByteUtils.subarray(split.get(i + 1), 0, 20);
        String hash = ByteUtils.bytesToHex(hashBytes);
        objects[i - 1] = new TreeObject(mode, fileName, hash);
        split.set(i + 1, ByteUtils.subarray(split.get(i + 1), 20));
        System.out.println(fileName);
      }
    } catch (IOException | DataFormatException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
