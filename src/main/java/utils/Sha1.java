package utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Sha1 {

  public static String hash(String input) {
    byte[] hash;
    try {
      hash = MessageDigest.getInstance("SHA-1")
          .digest(input.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
    return HexFormat.of().formatHex(hash);
  }

  public static String hash(byte[] input) {
    byte[] hash;
    try {
      hash = MessageDigest.getInstance("SHA-1")
          .digest(input);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
    return HexFormat.of().formatHex(hash);
  }
}
