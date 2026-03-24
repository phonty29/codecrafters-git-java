package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha1 {

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
