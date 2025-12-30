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

  public String value() {
    return mode;
  }

  public static Mode fromNumber(String mode) {
    for (Mode ft : Mode.values()) {
      if (ft.value().equals(mode)) {
        return ft;
      }
    }

    throw new IllegalArgumentException("Unknown mode " + mode);
  }
}
