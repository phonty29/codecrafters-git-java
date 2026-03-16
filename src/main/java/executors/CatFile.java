package executors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.DataFormatException;
import utils.Zlib;

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
    if (sha1.length() != 40) {
      throw new IllegalArgumentException("Hash length must be 40 characters");
    }

    String objectDir = sha1.substring(0, 2);
    String objectFile = sha1.substring(2);
    String path = ".git/objects/" + objectDir + "/" + objectFile;
    try {
      var bytes = Files.readAllBytes(Path.of(path));
      var output = new String(Zlib.decompressObject(bytes));
      output = output.substring(output.indexOf('\0') + 1);
      System.out.print(output);
    } catch (IOException | DataFormatException e) {
      throw new RuntimeException(e.getMessage());
    }
  }
}
