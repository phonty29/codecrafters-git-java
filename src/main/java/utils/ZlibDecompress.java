package utils;

import java.io.ByteArrayOutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class ZlibDecompress {

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

}
