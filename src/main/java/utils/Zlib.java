package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Zlib {

  public static byte[] decompressObject(byte[] compressed) throws DataFormatException {
    Inflater inflater = new Inflater(); // zlib header expected
    inflater.setInput(compressed);

    byte[] buffer = new byte[4096];
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    while (!inflater.finished()) {
      int count = inflater.inflate(buffer);
      out.write(buffer, 0, count);
    }

    inflater.end();
    return out.toByteArray();
  }

  public static byte[] decompressPackObject(ByteBuffer buffer) throws IOException {
    Inflater inflater = new Inflater();
    inflater.setInput(buffer.array(), buffer.position(), buffer.remaining());

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] writeBuffer = new byte[4096];

    while (!inflater.finished() && !inflater.needsInput()) {
      try {
        int count = inflater.inflate(writeBuffer);
        if (count == 0 && inflater.needsInput()) {
          break;
        }

        out.write(writeBuffer, 0, count);
      } catch (DataFormatException e) {
        throw new IOException(e);
      }
    }
    buffer.position(buffer.position() + inflater.getTotalIn());
    return out.toByteArray();
  }

  public static byte[] compress(byte[] input) {
    Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION); // zlib header
    deflater.setInput(input);
    deflater.finish();

    byte[] buffer = new byte[4096];
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    while (!deflater.finished()) {
      int count = deflater.deflate(buffer);
      out.write(buffer, 0, count);
    }

    deflater.end();
    return out.toByteArray();
  }
}
