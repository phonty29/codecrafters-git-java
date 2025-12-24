package struct;

public record GitObject(
    FileType type,
    String name,
    String hash
) {

  public enum FileType {
    REGULAR_FILE("100644"),
    EXECUTABLE_FILE("100755"),
    SYMLINK("120000"),
    DIRECTORY("40000");

    private final String mode;

    FileType(String mode) {
      this.mode = mode;
    }

    public String getMode() {
      return mode;
    }

    public static FileType fromMode(String mode) {
      for (FileType ft : FileType.values()) {
        if (ft.getMode().equals(mode)) {
          return ft;
        }
      }

      throw new IllegalArgumentException("Unknown mode " + mode);
    }
  }
}
