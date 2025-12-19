import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import utils.ZlibDecompress;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.err.println("Logs from your program will appear here!");

     final String command = args[0];

     switch (command) {
       case "init" -> {
         final File root = new File(".git");
         new File(root, "objects").mkdirs();
         new File(root, "refs").mkdirs();
         final File head = new File(root, "HEAD");

         try {
           head.createNewFile();
           Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
           System.out.println("Initialized git directory");
         } catch (IOException e) {
           throw new RuntimeException(e);
         }
       }
       case "cat-file" -> {
        if (args.length < 3) {
          throw new IllegalArgumentException("Few command line arguments for `cat-file`");
        }
        final String parameter = args[1];
        final String hash = args[2];
        if (hash.length() != 40) {
          throw new IllegalArgumentException("Hash length must be 40 characters");
        }

        String objectDir = hash.substring(0, 2);
        String objectFile = hash.substring(2);
        String path = ".git/objects/" + objectDir + "/" + objectFile;
        try {
          var bytes = Files.readAllBytes(Path.of(path));
          var output = new String(ZlibDecompress.decompress(bytes));
          output = output.substring(output.indexOf('\0')+1);
          System.out.print(output);
        } catch (IOException | DataFormatException e) {
          throw new RuntimeException(e.getMessage());
        }
       }
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
