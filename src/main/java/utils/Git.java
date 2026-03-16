package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import struct.Mode;
import struct.ObjectType;

public class Git {
  private static final Map<String, ObjectType> hashTypeMap = new HashMap<>();

  public static byte[] createBlob(Path readPath) throws IOException {
    byte[] contentBytes = Files.readAllBytes(readPath);
    byte size = (byte) contentBytes.length;
    byte[] headerBytes = ("blob " + size + '\0').getBytes(StandardCharsets.UTF_8);
    byte[] blobBytes = new byte[headerBytes.length + contentBytes.length];
    System.arraycopy(headerBytes, 0, blobBytes, 0, headerBytes.length);
    System.arraycopy(contentBytes, 0, blobBytes, headerBytes.length, contentBytes.length);
    return createGitObject(blobBytes);
  }

  public static byte[] createGitObject(byte[] content) throws IOException {
    byte[] hashBytes = Sha1.hash(content);
    String hash = HexFormat.of().formatHex(hashBytes);
    String objectDir = hash.substring(0, 2);
    String objectFile = hash.substring(2);
    String writePath = ".git/objects/" + objectDir + "/" + objectFile;
    byte[] compressedContent = Zlib.compress(content);

    Path path = Paths.get(writePath);
    Path parentDir = path.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      Files.createDirectories(parentDir);
    }
    Files.createFile(path);
    Files.write(path, compressedContent);
    return hashBytes;
  }

  public static byte[] createTree(Path path) throws IOException {
    if (Files.notExists(path) || !Files.isDirectory(path)) {
      throw new IllegalArgumentException(path + " is not a directory or doesn't exist");
    }
    List<Path> entries = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
      for (Path entry : stream) {
        entries.add(entry);
      }
    }
    entries.sort((a, b) -> {
      byte[] ba = a.getFileName().toString().getBytes(StandardCharsets.UTF_8);
      byte[] bb = b.getFileName().toString().getBytes(StandardCharsets.UTF_8);
      return Arrays.compareUnsigned(ba, bb);
    });

    ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
    entries.forEach(entry -> {
      try {
        if (Files.isHidden(entry)) {
          return;
        }
        if (Files.isDirectory(entry)) {
          byteOutStream.write(
              String.format("%s %s\0", Mode.DIRECTORY.value(), entry.getFileName()).getBytes(
                  StandardCharsets.UTF_8));
          byteOutStream.write(createTree(entry));
        } else if (Files.isRegularFile(entry)) {
          byteOutStream.write(
              String.format("%s %s\0", Mode.REGULAR_FILE.value(), entry.getFileName()).getBytes(
                  StandardCharsets.UTF_8));
          byteOutStream.write(createBlob(entry));
        } else if (Files.isSymbolicLink(entry)) {
          byteOutStream.write(
              String.format("%s %s\0", Mode.SYMLINK.value(), entry.getFileName()).getBytes(
                  StandardCharsets.UTF_8));
          byteOutStream.write(createBlob(entry));
        } else if (Files.isExecutable(entry)) {
          byteOutStream.write(
              String.format("%s %s\0", Mode.EXECUTABLE_FILE.value(), entry.getFileName()).getBytes(
                  StandardCharsets.UTF_8));
          byteOutStream.write(createBlob(entry));
        }
      } catch (IOException e) {
        System.err.println(e.getMessage());
      }
    });
    byte[] treeContent = byteOutStream.toByteArray();
    byte[] treePrefix = String.format("tree %s\0", treeContent.length)
        .getBytes(StandardCharsets.UTF_8);
    byte[] tree = ByteBuffer.allocate(treePrefix.length + treeContent.length)
        .put(treePrefix)
        .put(treeContent)
        .array();
    return createGitObject(tree);
  }

  static byte[] createObjectPayload(ObjectType type, byte[] content) {
    byte[] objectHeader = String.format("%s %s\0", type.toString(), content.length).getBytes(StandardCharsets.UTF_8);
    byte[] objectPayload = new byte[objectHeader.length + content.length];
    System.arraycopy(objectHeader, 0, objectPayload, 0, objectHeader.length);
    System.arraycopy(content, 0, objectPayload, objectHeader.length, content.length);
    return objectPayload;
  }

  static byte[] saveGitObject(ObjectType type, byte[] content) throws IOException {
    byte[] payload = createObjectPayload(type, content);
    return createGitObject(payload);
  }

  public static void processPackFile(ByteBuffer packBuffer) throws IOException {
    byte[] magicBytes = new byte[8];
    packBuffer.get(magicBytes);
    if (!new String(magicBytes, StandardCharsets.UTF_8).contentEquals("0008NAK\n")) {
      throw new IllegalArgumentException("Not an initial clone");
    }

    // Read packfile header
    packBuffer.position(packBuffer.position() + "PACK".length());
    int version = packBuffer.getInt();
    System.err.println("Git version: " + version);
    int objectCount = packBuffer.getInt();

    // Read packfile object entry
    for (int i = 0; i < objectCount; i++) {
      int objectHeader = packBuffer.get() & 0xFF;
      ObjectType objectType = ObjectType.fromValue((byte) ((objectHeader >> 4) & 0b111));
      int objectSize = objectHeader & 0x0F;
      int shift = 4;
      while ((objectHeader & 0x80) != 0) {
        objectHeader = packBuffer.get() & 0xFF;
        objectSize |= (objectHeader & 0b0111_1111) << shift;
        shift += 7;
      }
      byte[] objectPayload = processObject(objectType, packBuffer, objectSize);
      byte[] objectHash = saveGitObject(objectType, objectPayload);
    }
  }

  private static byte[] processObject(ObjectType objectType, ByteBuffer packBuffer, int objectSize) throws IOException {
    return switch (objectType) {
      case COMMIT, BLOB, TREE -> Zlib.decompressPackObject(packBuffer);
      case REF_DELTA -> processRefDeltaObject(packBuffer, objectSize);
      case OFS_DELTA -> processObsDeltaObject(packBuffer, objectSize);
      default -> throw new IllegalArgumentException("Object type is not supported: " + objectType);
    };
  }

  private static byte[] processRefDeltaObject(ByteBuffer packBuffer) throws IOException {
    byte[] baseHashObjectBytes = new byte[20];
    packBuffer.get(baseHashObjectBytes);
    byte[] refDeltaInstructions = Zlib.decompressPackObject(packBuffer);
    return Delta.applyDelta(ObjectType.REF_DELTA, baseHashObjectBytes, refDeltaInstructions);
  }

  private static byte[] processObsDeltaObject(ByteBuffer packBuffer) throws IOException {
    byte currentByte =  packBuffer.get();
    int offset = currentByte & 0x7f;

    while ((currentByte & 0x80) != 0) {
      currentByte = packBuffer.get();
      offset = ((offset + 1) << 7) | (currentByte & 0x7f);
    }
    byte[] obsDeltaInstructions = Zlib.decompressPackObject(packBuffer);
    return Delta.applyDelta(ObjectType.OFS_DELTA, new byte[0], obsDeltaInstructions);
  }
}
