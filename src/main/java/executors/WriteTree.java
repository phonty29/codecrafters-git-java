package executors;

import static utils.GitUtils.createTree;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;

public class WriteTree implements Executor {

  @Override
  public void execute() {
    try {
      Path path = Paths.get("./");
      byte[] sha1Hash = createTree(path);
      System.out.println(HexFormat.of().formatHex(sha1Hash));
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
