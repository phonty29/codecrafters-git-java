import commands.CatFile;
import commands.Clone;
import commands.CommitTree;
import commands.HashObject;
import commands.Init;
import commands.LsTree;
import commands.WriteTree;
import java.nio.file.Path;

public class Main {

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

    final String command = args[0];

    switch (command) {
      case "init" -> new Init(Path.of("./")).execute();
      case "cat-file" -> new CatFile(args).execute();
      case "hash-object" -> new HashObject(args).execute();
      case "ls-tree" -> new LsTree(args).execute();
      case "write-tree" -> new WriteTree().execute();
      case "commit-tree" -> new CommitTree(args).execute();
      case "clone" -> new Clone(args).execute();
      default -> System.out.println("Unknown command: " + command);
    }
  }
}
