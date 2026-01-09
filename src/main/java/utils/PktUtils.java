package utils;

import java.util.Objects;

public class PktUtils {
  public static int PKT_LINE_SIZE_HEADER = 4;
  public static String PKT_FLUSH = "0000";
  public static String HEAD = "HEAD";

  public static String retrieveHeadShaFromPktFormattedRefs(String pktText) {
    if (Objects.isNull(pktText) || pktText.isEmpty()) {
      throw new IllegalArgumentException("pkt text is null or empty");
    }
    String[] splitPktLine = pktText.split(PKT_FLUSH);
    if (splitPktLine.length < 2) {
      throw new IllegalArgumentException("pkt text doesn't contain payload");
    }
    String[] pktPayloads = splitPktLine[1].split("\n");

    String firstPktLine = pktPayloads[0];
    String[] firstPktLineSplit = firstPktLine.split("\0");
    if (firstPktLineSplit.length < 2) {
      throw new IllegalArgumentException("first ref should contain capabilities");
    }
    String firstPktLineHeader = firstPktLineSplit[0];
    if (firstPktLineHeader.split(" ")[1].contains(HEAD)) {
      return firstPktLineHeader.split(" ")[0].substring(PKT_LINE_SIZE_HEADER);
    }
    for (int i = 1; i < pktPayloads.length; i++) {
      String pktLine = pktPayloads[i];
      String[] pktLineSplit = pktLine.split(" ");
      if (pktLineSplit[1].contains(HEAD)) {
        return firstPktLineHeader.split(" ")[0].substring(PKT_LINE_SIZE_HEADER);
      }
    }

    throw new IllegalArgumentException("pkt text doesn't contain sha for HEAD");
  }

  public static byte[] createPktNegotiationPayload(String headSha) {
    String wantPktLine = pktLine("want " + headSha + " ofs-delta\n");
    String donePktLine = pktLine("done\n");
    String negotiationPayload = wantPktLine + PKT_FLUSH + donePktLine;
    return negotiationPayload.getBytes();
  }

  private static String pktLine(String content) {
    int length = content.length() + PKT_LINE_SIZE_HEADER;
    return String.format("%04X", length) + content;
  }
}
