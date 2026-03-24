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
import java.util.zip.DataFormatException;
import struct.Mode;
import struct.ObjectType;

public class Git {

  private static final Map<byte[], ObjectType> hashTypeMap = new HashMap<>();
  private static final Map<Integer, byte[]> positionObjectHashMap = new HashMap<>();

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

  public static byte[] createGitObject(Path targetPath, byte[] content) throws IOException {
    byte[] hashBytes = Sha1.hash(content);
    String hash = HexFormat.of().formatHex(hashBytes);
    String objectDir = hash.substring(0, 2);
    String objectFile = hash.substring(2);
    Path path = targetPath.resolve(".git/objects/" + objectDir + "/" + objectFile);
    byte[] compressedContent = Zlib.compress(content);

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

  public static byte[] createObjectPayload(ObjectType type, byte[] content) {
    byte[] objectHeader = String.format("%s %s\0", type.toString(), content.length).getBytes(StandardCharsets.UTF_8);
    byte[] objectPayload = new byte[objectHeader.length + content.length];
    System.arraycopy(objectHeader, 0, objectPayload, 0, objectHeader.length);
    System.arraycopy(content, 0, objectPayload, objectHeader.length, content.length);
    return objectPayload;
  }

  public static byte[] getGitObject(Path targetPath, byte[] hash) {
    String sha1 = HexFormat.of().formatHex(hash);
    String objectDir = sha1.substring(0, 2);
    String objectFile = sha1.substring(2);
    Path path = targetPath.resolve(".git/objects/" + objectDir + "/" + objectFile);
    try {
      var bytes = Files.readAllBytes(path);
      return Zlib.decompressObject(bytes);
    } catch (IOException | DataFormatException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * @param packBuffer - remote Git packfile buffer
   * @return first commit hash
   */
  public static byte[] processPackFile(Path absolutePath, ByteBuffer packBuffer) throws IOException {
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
    byte[] firstCommitHash = new byte[0];

    // Read packfile object entry
    for (int i = 0; i < objectCount; i++) {
      int objectStartPosition = packBuffer.position();
      int objectHeader = packBuffer.get() & 0xFF;
      ObjectType objectType = ObjectType.fromValue((byte) ((objectHeader >> 4) & 0b111));
      int objectSize = objectHeader & 0x0F;
      int shift = 4;
      while ((objectHeader & 0x80) != 0) {
        objectHeader = packBuffer.get() & 0xFF;
        objectSize |= (objectHeader & 0b0111_1111) << shift;
        shift += 7;
      }
      byte[] objectHash = processObject(objectType, objectStartPosition, packBuffer, absolutePath);
      if (firstCommitHash.length == 0 && objectType == ObjectType.COMMIT) {
        firstCommitHash = objectHash;
      }
    }

    return firstCommitHash;
  }

  private static byte[] processObject(ObjectType objectType, int objectStartPosition, ByteBuffer packBuffer,
      Path absolutePath) throws IOException {
    return switch (objectType) {
      case COMMIT, BLOB, TREE -> {
        byte[] payload = createObjectPayload(objectType, Zlib.decompressPackObject(packBuffer));
        byte[] hash = createGitObject(absolutePath, payload);
        positionObjectHashMap.put(objectStartPosition, hash);
        hashTypeMap.put(hash, objectType);
        yield hash;
      }
      case REF_DELTA -> processRefDeltaObject(objectStartPosition, packBuffer, absolutePath);
      case OFS_DELTA -> processObsDeltaObject(objectStartPosition, packBuffer, absolutePath);
      default -> throw new IllegalArgumentException("Object type is not supported: " + objectType);
    };
  }

  private static byte[] processRefDeltaObject(int objectStartPosition, ByteBuffer packBuffer, Path absolutePath)
      throws IOException {
    byte[] baseObjectHash = new byte[20];
    packBuffer.get(baseObjectHash);
    ObjectType refObjectType = hashTypeMap.get(baseObjectHash);

    byte[] refDeltaInstructions = Zlib.decompressPackObject(packBuffer);
    byte[] baseObject = getGitObject(absolutePath, baseObjectHash);
    byte[] baseContent = removeGitHeader(baseObject);
    byte[] resultObject = Delta.applyDelta(baseContent, refDeltaInstructions);
    byte[] objectPayload = createObjectPayload(refObjectType, resultObject);
    byte[] resultHash = createGitObject(objectPayload);
    positionObjectHashMap.put(objectStartPosition, resultHash);
    hashTypeMap.put(resultHash, refObjectType);
    return resultHash;
  }

  private static byte[] processObsDeltaObject(int objectStartPosition, ByteBuffer packBuffer, Path absolutePath)
      throws IOException {
    byte currentByte = packBuffer.get();
    int offset = currentByte & 0x7f;
    while ((currentByte & 0x80) != 0) {
      currentByte = packBuffer.get();
      offset = ((offset + 1) << 7) | (currentByte & 0x7f);
    }
    int baseObjectPosition = objectStartPosition - offset;
    byte[] baseObjectHash = positionObjectHashMap.get(baseObjectPosition);
    ObjectType baseObjectType = hashTypeMap.get(baseObjectHash);

    byte[] obsDeltaInstructions = Zlib.decompressPackObject(packBuffer);
    byte[] baseObject = getGitObject(absolutePath, baseObjectHash);
    byte[] baseContent = removeGitHeader(baseObject);
    byte[] resultObject = Delta.applyDelta(baseContent, obsDeltaInstructions);
    byte[] objectPayload = createObjectPayload(baseObjectType, resultObject);
    byte[] resultHash = createGitObject(absolutePath, objectPayload);
    positionObjectHashMap.put(objectStartPosition, resultHash);
    hashTypeMap.put(resultHash, baseObjectType);
    return resultHash;
  }

  public static byte[] removeGitHeader(byte[] data) {
    int i = 0;
    while (i < data.length && data[i] != 0) {
      i++;
    }
    if (i == data.length) {
      throw new IllegalStateException("Invalid git object (no header)");
    }
    int contentStart = i + 1;
    return Arrays.copyOfRange(data, contentStart, data.length);
  }
}
