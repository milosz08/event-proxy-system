package pl.miloszgilga.event.proxy.server;

import java.util.function.BiFunction;
import java.util.function.Function;

public class TestData {
  public static final Function<String, TestDataPayload> NAS = mockSuffix -> new TestDataPayload(
    "dlink-02C972_E-Mail_Alert",
    """
      A SMART Test Was Performed On The Following Hard Drive At 03:2:10 On 27-July-2025.

      Device Model:  WDC WD20EFZX-68AWUN0
      Serial Number:  RESTRICTED
      Size: 2,000G

      The Result Of The Test Is: Pass

      Sincerely,
      Your dlink-02C972
      """,
    generateMockSuffix("NAS", mockSuffix)
  );

  public static final BiFunction<String, String, TestDataPayload> DVR = (camNumber, mockSuffix) ->
    new TestDataPayload(
      String.format("Embedded Net DVR: Motion Detected On Channel A%s", camNumber),
      String.format("""
        This is an automatically generated e-mail from your DVR.

        EVENT TYPE:    Motion Detected
        EVENT TIME:    2025-07-26,16:53:09
        DVR NAME:      Embedded Net DVR
        DVR S/N:       RESTRICTED
        CAMERA NAME(NUM):   CAM %s Garage(A%s)
        """, camNumber, camNumber),
      generateMockSuffix("DVR", mockSuffix)
    );

  private TestData() {
  }

  public static String generateMockSuffix(String name, String mockSuffix) {
    return String.format("%s%s", name, mockSuffix != null ? " " + mockSuffix : "");
  }
}
