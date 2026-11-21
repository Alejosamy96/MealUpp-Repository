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

  @Test
  void readsNestedJson() {
    Object tree =
        MiniJson.parseTree("{\"a\":{\"b\":[1,2.5,\"x\",true,false,null,{\"c\":\"d\"}]},\"e\":[]}");

    assertThat(tree).isInstanceOf(Map.class);
    Map<?, ?> root = (Map<?, ?>) tree;
    assertThat(root.get("e")).isEqualTo(List.of());
    List<?> items = (List<?>) ((Map<?, ?>) root.get("a")).get("b");
    assertThat(items).hasSize(7);
    assertThat(items.get(0)).isEqualTo(new BigDecimal("1"));
    assertThat(items.get(1)).isEqualTo(new BigDecimal("2.5"));
    assertThat(items.get(2)).isEqualTo("x");
    assertThat(items.get(3)).isEqualTo(true);
    assertThat(items.get(4)).isEqualTo(false);
    assertThat(items.get(5)).isNull();
    assertThat(((Map<?, ?>) items.get(6)).get("c")).isEqualTo("d");
  }

  @Test
  void readsTopLevelScalarsAndEmptyContainers() {
    assertThat(MiniJson.parseTree("\"hola\"")).isEqualTo("hola");
    assertThat(MiniJson.parseTree(" 12 ")).isEqualTo(new BigDecimal("12"));
    assertThat(MiniJson.parseTree("{}")).isEqualTo(Map.of());
    assertThat(MiniJson.parseTree("[ ]")).isEqualTo(List.of());
  }

  @Test
  void rejectsBrokenNestedJson() {
    assertThatThrownBy(() -> MiniJson.parseTree("")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseTree("{\"a\":1} x"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseTree("{\"a\":1 \"b\":2}"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseTree("[1 2]"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseTree("[1,"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> MiniJson.parseTree("nulo"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
