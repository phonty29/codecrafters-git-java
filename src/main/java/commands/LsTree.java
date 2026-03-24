package commands;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.DataFormatException;
import struct.Mode;
import struct.TreeObject;
import utils.ByteUtils;
import utils.Git;
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
    final String treeSha = params[2];
    String objectDir = treeSha.substring(0, 2);
    String objectFile = treeSha.substring(2);
    Path path = Path.of(".git/objects/" + objectDir + "/" + objectFile);
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
      result.forEach(t -> System.out.println(t.name()));
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
