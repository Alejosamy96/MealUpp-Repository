package com.example.mealupp.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HmacSignerTest {

  @Test
  void matchesTheKnownHmacSha256Vector() {
    assertThat(HmacSigner.hmacHex("key", "The quick brown fox jumps over the lazy dog"))
        .isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
  }

  @Test
  void acceptsTheRightSignatureWithOrWithoutPrefix() {
    String signature = HmacSigner.hmacHex("s3cret", "{\"a\":1}");

    assertThat(HmacSigner.matches("s3cret", "{\"a\":1}", signature)).isTrue();
    assertThat(HmacSigner.matches("s3cret", "{\"a\":1}", "sha256=" + signature.toUpperCase()))
        .isTrue();
  }

  @Test
  void rejectsAChangedBodyAnotherKeyOrMissingData() {
    String signature = HmacSigner.hmacHex("s3cret", "{\"a\":1}");

    assertThat(HmacSigner.matches("s3cret", "{\"a\":2}", signature)).isFalse();
    assertThat(HmacSigner.matches("otra", "{\"a\":1}", signature)).isFalse();
    assertThat(HmacSigner.matches("s3cret", "{\"a\":1}", null)).isFalse();
    assertThat(HmacSigner.matches("s3cret", "{\"a\":1}", "")).isFalse();
    assertThat(HmacSigner.matches("", "{\"a\":1}", signature)).isFalse();
    assertThat(HmacSigner.matches(null, "{\"a\":1}", signature)).isFalse();
  }
}
