package utils;

import java.nio.ByteBuffer;

public class Delta {

  public static byte[] applyDelta(byte[] baseObject, byte[] delta) {
    ByteBuffer buf = ByteBuffer.wrap(delta);

    long baseSize = readVarInt(buf);
    long resultSize = readVarInt(buf);

    if (baseSize != baseObject.length) {
      throw new IllegalStateException(
          "Base size mismatch: expected " + baseSize + ", got " + baseObject.length
      );
    }

    byte[] result = new byte[(int) resultSize];
    int resultPos = 0;

    while (buf.hasRemaining()) {
      int opcode = buf.get() & 0xFF;

      if ((opcode & 0x80) != 0) {
        // COPY from baseObject
        int offset = 0;
        int size = 0;

        // offset (up to 4 bytes)
        if ((opcode & 0x01) != 0) offset |= (buf.get() & 0xFF);
        if ((opcode & 0x02) != 0) offset |= (buf.get() & 0xFF) << 8;
        if ((opcode & 0x04) != 0) offset |= (buf.get() & 0xFF) << 16;
        if ((opcode & 0x08) != 0) offset |= (buf.get() & 0xFF) << 24;

        // size (up to 3 bytes)
        if ((opcode & 0x10) != 0) size |= (buf.get() & 0xFF);
        if ((opcode & 0x20) != 0) size |= (buf.get() & 0xFF) << 8;
        if ((opcode & 0x40) != 0) size |= (buf.get() & 0xFF) << 16;

        if (size == 0) {
          size = 0x10000; // 65536
        }

        // bounds check (important!)
        if (offset + size > baseObject.length) {
          throw new IllegalStateException("Invalid delta copy range");
        }

        System.arraycopy(baseObject, offset, result, resultPos, size);
        resultPos += size;

      } else if (opcode != 0) {
        // INSERT literal
        int size = opcode;

        if (size > buf.remaining()) {
          throw new IllegalStateException("Invalid delta insert size");
        }

        buf.get(result, resultPos, size);
        resultPos += size;

      } else {
        // opcode == 0 is invalid in Git delta
        throw new IllegalStateException("Invalid delta opcode 0");
      }
    }

    if (resultPos != result.length) {
      throw new IllegalStateException(
          "Delta result size mismatch: expected " + result.length + ", got " + resultPos
      );
    }

    return result;
  }

  private static long readVarInt(ByteBuffer buf) {
    long result = 0;
    int shift = 0;

    while (true) {
      int b = buf.get() & 0xFF;
      result |= (long)(b & 0x7F) << shift;

      if ((b & 0x80) == 0) {
        break;
      }

      shift += 7;
    }

    return result;
  }
}
