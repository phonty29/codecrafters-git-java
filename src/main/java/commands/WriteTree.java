package commands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import git.Git;

public class WriteTree implements Executor {

  @Override
  public void execute() {
    try {
      Path path = Paths.get("./");
      byte[] sha1 = new Git(path).createTree(path);
      System.out.println(HexFormat.of().formatHex(sha1));
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
