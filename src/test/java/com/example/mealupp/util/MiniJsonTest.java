package com.example.mealupp.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MiniJsonTest {

  @Test
  void readsAFlatObjectWithTextNumbersAndBooleans() {
    Map<String, String> data =
        MiniJson.parseFlat("{\"sub\":\"ana\", \"exp\": 1700000000 ,\"ok\":true,\"n\":null}");

    assertThat(data)
        .containsEntry("sub", "ana")
        .containsEntry("exp", "1700000000")
        .containsEntry("ok", "true")
        .containsEntry("n", "null");
  }

  @Test
  void understandsEscapesAndUnicode() {
    Map<String, String> data = MiniJson.parseFlat("{\"a\":\"x\\\"y\\\\z\\n\",\"u\":\"\\u00e1\"}");

    assertThat(data.get("a")).isEqualTo("x\"y\\z\n");
    assertThat(data.get("u")).isEqualTo("á");
  }

  @Test
  void readsAnEmptyObject() {
    assertThat(MiniJson.parseFlat("{ }")).isEmpty();
  }

  @Test
  void rejectsNestedObjectsLeftoversAndBrokenText() {
    assertThatThrownBy(() -> MiniJson.parseFlat("{\"a\":{\"b\":1}}"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseFlat("{\"a\":1} x"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseFlat("{\"a\":\"sin cerrar}"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseFlat("{\"a\" 1}"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseFlat("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseFlat(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void writesMapsListsNumbersAndEscapes() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("texto", "a\"b\n");
    map.put("entero", 3);
    map.put("valor", new BigDecimal("24000"));
    map.put("lista", List.of(1, "x"));
    map.put("nulo", null);
    map.put("hora", LocalTime.of(12, 30));
    map.put("booleano", true);

    assertThat(MiniJson.write(map))
        .isEqualTo(
            "{\"texto\":\"a\\\"b\\n\",\"entero\":3,\"valor\":24000,\"lista\":[1,\"x\"],"
                + "\"nulo\":null,\"hora\":\"12:30\",\"booleano\":true}");
  }

  @Test
  void whatItWritesItCanReadBack() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("nombre", "Pasta \"Alfredo\"\ncon \\ barra");

    assertThat(MiniJson.parseFlat(MiniJson.write(map)))
        .containsEntry("nombre", "Pasta \"Alfredo\"\ncon \\ barra");
  }
}
