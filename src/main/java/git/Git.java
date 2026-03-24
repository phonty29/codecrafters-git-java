package git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.DataFormatException;
import struct.Mode;
import struct.ObjectType;
import struct.PackObject;
import struct.TreeObject;
import utils.Delta;
import utils.Sha1;
import utils.Zlib;

public class Git {

  private final Path root;
  private final Map<byte[], ObjectType> sha1TypeMap = new HashMap<>();
  private final Map<Integer, byte[]> offsetSha1Map = new HashMap<>();

  public Git(Path root) {
    this.root = root;
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

  public Path getRoot() {
    return this.root;
  }

  public byte[] createBlob(Path readPath) throws IOException {
    byte[] contentBytes = Files.readAllBytes(root.resolve(readPath));
    byte size = (byte) contentBytes.length;
    byte[] headerBytes = ("blob " + size + '\0').getBytes(StandardCharsets.UTF_8);
    byte[] blobBytes = new byte[headerBytes.length + contentBytes.length];
    System.arraycopy(headerBytes, 0, blobBytes, 0, headerBytes.length);
    System.arraycopy(contentBytes, 0, blobBytes, headerBytes.length, contentBytes.length);
    return createGitObject(blobBytes);
  }

  public byte[] createGitObject(byte[] content) throws IOException {
    byte[] hashBytes = Sha1.hash(content);
    String hash = HexFormat.of().formatHex(hashBytes);
    String objectDir = hash.substring(0, 2);
    String objectFile = hash.substring(2);
    Path path = root.resolve(".git/objects/" + objectDir + "/" + objectFile);
    byte[] compressedContent = Zlib.compress(content);

    Path parentDir = path.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      Files.createDirectories(parentDir);
    }
    Files.createFile(path);
    Files.write(path, compressedContent);
    return hashBytes;
  }

  public List<TreeObject> readTree(String treeSha) throws IOException, DataFormatException {
    String objectDir = treeSha.substring(0, 2);
    String objectFile = treeSha.substring(2);
    Path path = root.resolve(".git/objects/" + objectDir + "/" + objectFile);
    byte[] bytes = Files.readAllBytes(path);
    byte[] decompressed = Zlib.decompressObject(bytes);
    byte[] data = Git.removeGitHeader(decompressed);
    List<TreeObject> result = new ArrayList<>();
    int pos = 0;
    while (pos < data.length) {
      int space = findByte(data, pos, (byte) ' ');
      String modeStr = new String(data, pos, space - pos);
      Mode mode = Mode.fromNumber(modeStr);
      pos = space + 1;
      int nullByte = findByte(data, pos, (byte) 0);
      String fileName = new String(data, pos, nullByte - pos);
      pos = nullByte + 1;
      byte[] shaBytes = Arrays.copyOfRange(data, pos, pos + 20);
      String sha = HexFormat.of().formatHex(shaBytes);
      pos += 20;
      result.add(new TreeObject(mode, fileName, sha));
    }
    return result;
  }

  private int findByte(byte[] data, int start, byte target) {
    for (int i = start; i < data.length; i++) {
      if (data[i] == target) {
        return i;
      }
    }
    throw new IllegalStateException("Byte not found");
  }

  public byte[] createTree(Path path) throws IOException {
    if (Objects.isNull(path)) {
      path = root;
    }
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

  private byte[] createObjectPayload(ObjectType type, byte[] content) {
    byte[] objectHeader = String.format("%s %s\0", type.toString(), content.length).getBytes(StandardCharsets.UTF_8);
    byte[] objectPayload = new byte[objectHeader.length + content.length];
    System.arraycopy(objectHeader, 0, objectPayload, 0, objectHeader.length);
    System.arraycopy(content, 0, objectPayload, objectHeader.length, content.length);
    return objectPayload;
  }

  public byte[] readGitObject(byte[] hash) throws IOException, DataFormatException {
    String sha1 = HexFormat.of().formatHex(hash);
    String objectDir = sha1.substring(0, 2);
    String objectFile = sha1.substring(2);
    Path path = root.resolve(".git/objects/" + objectDir + "/" + objectFile);
    var bytes = Files.readAllBytes(path);
    return Zlib.decompressObject(bytes);
  }

  public byte[] processPackFile(ByteBuffer packBuffer) throws IOException, DataFormatException {
    byte[] magicBytes = new byte[8];
    packBuffer.get(magicBytes);
    if (!new String(magicBytes, StandardCharsets.UTF_8).contentEquals("0008NAK\n")) {
      throw new IllegalArgumentException("Not an initial clone");
    }

    // Read packfile header
    packBuffer.position(packBuffer.position() + "PACK".length());
    int version = packBuffer.getInt();
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
      PackObject packObject = new PackObject(objectType, objectStartPosition);
      byte[] objectHash = processObject(packObject, packBuffer);
      if (firstCommitHash.length == 0 && objectType == ObjectType.COMMIT) {
        firstCommitHash = objectHash;
      }
    }

    return firstCommitHash;
  }

  private byte[] processObject(PackObject packObject, ByteBuffer packBuffer) throws IOException, DataFormatException {
    return switch (packObject.type()) {
      case COMMIT, BLOB, TREE -> saveObject(Zlib.decompressPackObject(packBuffer), packObject);
      case REF_DELTA -> processRefDeltaObject(packObject.offset(), packBuffer);
      case OFS_DELTA -> processObsDeltaObject(packObject.offset(), packBuffer);
      default -> throw new IllegalArgumentException("Object type is not supported: " + packObject.type());
    };
  }

  private byte[] processRefDeltaObject(int packObjectOffset, ByteBuffer packBuffer)
      throws IOException, DataFormatException {
    byte[] baseObjectHash = new byte[20];
    packBuffer.get(baseObjectHash);
    return processDeltaObject(packObjectOffset, packBuffer, baseObjectHash);
  }

  private byte[] processObsDeltaObject(int packObjectOffset, ByteBuffer packBuffer)
      throws IOException, DataFormatException {
    byte currentByte = packBuffer.get();
    int offset = currentByte & 0x7f;
    while ((currentByte & 0x80) != 0) {
      currentByte = packBuffer.get();
      offset = ((offset + 1) << 7) | (currentByte & 0x7f);
    }
    int baseObjectPosition = packObjectOffset - offset;
    byte[] baseObjectHash = offsetSha1Map.get(baseObjectPosition);
    return processDeltaObject(packObjectOffset, packBuffer, baseObjectHash);
  }

  private byte[] processDeltaObject(int offset, ByteBuffer packBuffer, byte[] baseObjectHash)
      throws IOException, DataFormatException {
    ObjectType baseObjectType = sha1TypeMap.get(baseObjectHash);
    byte[] obsDeltaInstructions = Zlib.decompressPackObject(packBuffer);
    byte[] baseObject = readGitObject(baseObjectHash);
    byte[] baseContent = removeGitHeader(baseObject);
    byte[] resultContent = Delta.applyDelta(baseContent, obsDeltaInstructions);
    PackObject basePackObject = new PackObject(baseObjectType, offset);
    return saveObject(resultContent, basePackObject);
  }

  private byte[] saveObject(byte[] content, PackObject packObject) throws IOException {
    byte[] objectPayload = createObjectPayload(packObject.type(), content);
    byte[] resultHash = createGitObject(objectPayload);
    offsetSha1Map.put(packObject.offset(), resultHash);
    sha1TypeMap.put(resultHash, packObject.type());
    return resultHash;
  }
}
