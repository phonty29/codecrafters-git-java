package executors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Clone {
  private final String[] args;

  public Clone(String... args) {
    this.args = args;
  }

  public void execute() {
    if (args.length < 3) {
      throw new IllegalArgumentException("Too few arguments in `clone` command");
    }
    var gitDir = args[2];
    try {
      Path absolutePath = Files.createDirectory(Paths.get(gitDir));
      new Init(absolutePath).execute();
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }
  }
}
