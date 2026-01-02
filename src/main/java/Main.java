import executors.CatFile;
import executors.Clone;
import executors.CommitTree;
import executors.HashObject;
import executors.Init;
import executors.LsTree;
import executors.WriteTree;
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
