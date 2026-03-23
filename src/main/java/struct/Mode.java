package struct;

public enum Mode {
  REGULAR_FILE("100644"),
  EXECUTABLE_FILE("100755"),
  SYMLINK("120000"),
  DIRECTORY("40000");

  private final String mode;

  Mode(String mode) {
    this.mode = mode;
  }

  public static boolean isBlob(Mode mode) {
     return mode.equals(REGULAR_FILE) || mode.equals(EXECUTABLE_FILE) || mode.equals(SYMLINK);
  }

  public static Mode fromNumber(String mode) {
    for (Mode ft : Mode.values()) {
      if (ft.value().equals(mode)) {
        return ft;
      }
    }

    throw new IllegalArgumentException("Unknown mode " + mode);
  }

  public String value() {
    return mode;
  }
}
