package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public class ByteUtils {
  public static String bytesToHex(byte[] bytes) {
    return HexFormat.of().formatHex(bytes);
  }

  public static List<byte[]> split(byte[] data, byte delimiter) {
    List<byte[]> result = new ArrayList<>();
    int from = 0;

    for (int i = 0; i < data.length; i++) {
      if (data[i] == delimiter) {
        int len = i - from;
        byte[] part = new byte[len];
        System.arraycopy(data, from, part, 0, len);
        result.add(part);
        from = i + 1;
      }
    }

    int len = data.length - from;
    byte[] part = new byte[len];
    System.arraycopy(data, from, part, 0, len);
    result.add(part);

    return result;
  }

  public static byte[] subarray(byte[] input, int from, int to) {
    return Arrays.copyOfRange(input, from, to);
  }


  public static byte[] subarray(byte[] input, int from) {
    return Arrays.copyOfRange(input, from, input.length);
  }
}
