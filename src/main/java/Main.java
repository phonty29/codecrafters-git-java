import executors.CatFile;
import executors.Init;
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
       case "init" -> new Init().execute();
       case "cat-file" -> new CatFile(args).execute();
       default -> System.out.println("Unknown command: " + command);
     }
  }
}
