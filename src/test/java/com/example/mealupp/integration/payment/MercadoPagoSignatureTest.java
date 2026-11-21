package com.example.mealupp.integration.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mealupp.util.HmacSigner;
import org.junit.jupiter.api.Test;

class MercadoPagoSignatureTest {

  private static final String SECRET = "clave-del-webhook";
  private static final String TS = "1742505638683";

  private static String header(String manifest) {
    return "ts=" + TS + ",v1=" + HmacSigner.hmacHex(SECRET, manifest);
  }

  @Test
  void acceptsAValidSignature() {
    String manifest = "id:123456;request-id:abc-1;ts:" + TS + ";";

    assertThat(MercadoPagoSignature.isValid(SECRET, header(manifest), "abc-1", "123456")).isTrue();
  }

  @Test
  void lowercasesAnAlphanumericId() {
    String manifest = "id:abcdef;request-id:abc-1;ts:" + TS + ";";

    assertThat(MercadoPagoSignature.isValid(SECRET, header(manifest), "abc-1", "ABCDEF")).isTrue();
  }

  @Test
  void skipsMissingParts() {
    String manifest = "ts:" + TS + ";";

    assertThat(MercadoPagoSignature.isValid(SECRET, header(manifest), null, null)).isTrue();
  }

  @Test
  void rejectsAWrongSignature() {
    assertThat(MercadoPagoSignature.isValid(SECRET, "ts=" + TS + ",v1=00", "abc-1", "123456"))
        .isFalse();
  }

  @Test
  void rejectsWhenTheDataChanged() {
    String manifest = "id:123456;request-id:abc-1;ts:" + TS + ";";

    assertThat(MercadoPagoSignature.isValid(SECRET, header(manifest), "abc-1", "999999")).isFalse();
  }

  @Test
  void rejectsMissingSecretOrHeaderOrParts() {
    String manifest = "id:123456;request-id:abc-1;ts:" + TS + ";";

    assertThat(MercadoPagoSignature.isValid("", header(manifest), "abc-1", "123456")).isFalse();
    assertThat(MercadoPagoSignature.isValid(null, header(manifest), "abc-1", "123456")).isFalse();
    assertThat(MercadoPagoSignature.isValid(SECRET, null, "abc-1", "123456")).isFalse();
    assertThat(MercadoPagoSignature.isValid(SECRET, "basura", "abc-1", "123456")).isFalse();
    assertThat(MercadoPagoSignature.isValid(SECRET, "ts=" + TS, "abc-1", "123456")).isFalse();
    assertThat(MercadoPagoSignature.isValid(SECRET, "v1=abc", "abc-1", "123456")).isFalse();
  }
}
