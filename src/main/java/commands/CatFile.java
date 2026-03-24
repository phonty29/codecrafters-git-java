package commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.zip.DataFormatException;
import git.Git;

public class CatFile implements Executor {

  private final String[] params;

  public CatFile(String... params) {
    this.params = params;
  }

  @Override
  public void execute() {
    if (params.length < 3) {
      throw new IllegalArgumentException("Few command line arguments for `cat-file`");
    }
    final String sha1 = params[2];
    try {
      byte[] content = new Git(Path.of("./")).readGitObject(HexFormat.of().parseHex(sha1));
      String output = new String(content);
      output = output.substring(output.indexOf('\0') + 1);
      System.out.print(output);
    } catch (IOException | DataFormatException e) {
      System.err.println("Error reading `cat-file`: " + e.getMessage());
    }
  }
}
