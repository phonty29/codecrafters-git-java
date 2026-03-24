package commands;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.DataFormatException;
import struct.TreeObject;
import git.Git;

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
    try {
      List<TreeObject> result = new Git(Path.of("./")).readTree(treeSha);
      result.forEach(t -> System.out.println(t.name()));
    } catch (IOException | DataFormatException e) {
      System.err.println(e.getMessage());
    }
  }
}
