package struct;

public enum ObjectType {
  COMMIT((byte) 1),
  TREE((byte) 2),
  BLOB((byte) 3),
  TAG((byte) 4),
  OFS_DELTA((byte) 6),
  REF_DELTA((byte) 7);

  private final byte value;

  ObjectType(byte value) {
    this.value = value;
  }

  public byte value() {
    return value;
  }

  public String toString() {
    return switch (this) {
      case COMMIT -> "commit";
      case TREE -> "tree";
      case BLOB ->"blob";
      default -> throw new IllegalArgumentException(this.name() + " doesn't have String value");
    };
  }

  public static ObjectType fromValue(byte value) {
    for (ObjectType ot : ObjectType.values()) {
      if (ot.value() == value) {
        return ot;
      }
    }

    throw new IllegalArgumentException("Unknown object type: " + value);
  }
}
