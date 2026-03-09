package utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class Zlib {

  public static byte[] decompress(byte[] compressed) throws DataFormatException {
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

  public static byte[] decompress(ByteBuffer compressed) throws IOException {
    InputStream bufInStream = new InputStream() {
      @Override
      public int read() {
        if (compressed.hasRemaining()) {
          return compressed.get() & 0xFF;
        }
        return -1;
      }
    };
    InflaterInputStream inflaterInStream = new InflaterInputStream(bufInStream);
    return inflaterInStream.readAllBytes();
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
