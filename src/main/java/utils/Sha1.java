package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Sha1 {

  public static String hashAsString(byte[] input) {
    byte[] hash = hash(input);
    return HexFormat.of().formatHex(hash);
  }

  public static byte[] hash(byte[] input) {
    byte[] hash;
    try {
      hash = MessageDigest.getInstance("SHA-1")
          .digest(input);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
    return hash;
  }
}
