package com.example.mealupp.integration.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class SigV4SignerTest {

  /** Ejemplo oficial de la documentación de AWS (GET Object con encabezado Range). */
  @Test
  void matchesTheOfficialAwsExample() {
    SigV4Signer signer =
        new SigV4Signer(
            "AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY", "us-east-1", "s3");
    String emptyHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    TreeMap<String, String> headers = new TreeMap<>();
    headers.put("host", "examplebucket.s3.amazonaws.com");
    headers.put("range", "bytes=0-9");
    headers.put("x-amz-content-sha256", emptyHash);
    headers.put("x-amz-date", "20130524T000000Z");

    String authorization =
        signer.authorization("GET", "/test.txt", "", headers, emptyHash, "20130524T000000Z");

    assertThat(authorization)
        .isEqualTo(
            "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request, "
                + "SignedHeaders=host;range;x-amz-content-sha256;x-amz-date, "
                + "Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41");
  }

  @Test
  void sha256OfNothingIsTheKnownValue() {
    assertThat(SigV4Signer.sha256Hex(new byte[0]))
        .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
  }

  @Test
  void encodesThePathKeepingSlashesAndEscapingTheRest() {
    assertThat(SigV4Signer.encodePath("/b/dishes/1/ab_c-d.jpg"))
        .isEqualTo("/b/dishes/1/ab_c-d.jpg");
    assertThat(SigV4Signer.encodePath("/b/a b")).isEqualTo("/b/a%20b");
    assertThat(SigV4Signer.encodePath("/b/ñ")).isEqualTo("/b/%C3%B1");
  }
}
