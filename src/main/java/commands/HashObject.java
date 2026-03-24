package commands;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HexFormat;
import utils.Git;

public class HashObject implements Executor {

  private final String[] params;

  public HashObject(String... params) {
    this.params = params;
  }

  @Override
  public void execute() {
    if (params.length < 3) {
      throw new IllegalArgumentException("Few command line arguments for `hash-object`");
    }
    final String readPath = params[2];
    try {
      byte[] sha1 = Git.createBlob(Paths.get(readPath));
      System.out.println(HexFormat.of().formatHex(sha1));
    } catch (IOException ex) {
      System.err.println(ex.getMessage());
    }
  }
}
