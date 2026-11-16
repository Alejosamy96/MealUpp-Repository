package com.example.mealupp.util;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON mínimo, sin dependencias, para dos usos puntuales: leer objetos planos (los datos del token
 * y del webhook de pagos) y escribir los mensajes que se publican por WebSocket.
 */
public final class MiniJson {

  private MiniJson() {}

  /** Lee un objeto JSON plano. Todos los valores se devuelven como texto. */
  public static Map<String, String> parseFlat(String json) {
    if (json == null || json.isBlank()) {
      throw new IllegalArgumentException("JSON vacío");
    }
    return new Parser(json).parseObject();
  }

  /** Escribe un valor como JSON: mapas, listas, textos, números, booleanos y nulos. */
  public static String write(Object value) {
    StringBuilder sb = new StringBuilder();
    writeValue(sb, value);
    return sb.toString();
  }

  private static void writeValue(StringBuilder sb, Object value) {
    if (value == null) {
      sb.append("null");
    } else if (value instanceof String text) {
      writeString(sb, text);
    } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
      sb.append(value);
    } else if (value instanceof BigDecimal decimal) {
      sb.append(decimal.toPlainString());
    } else if (value instanceof Number number) {
      sb.append(number);
    } else if (value instanceof Map<?, ?> map) {
      sb.append('{');
      boolean first = true;
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        writeString(sb, String.valueOf(entry.getKey()));
        sb.append(':');
        writeValue(sb, entry.getValue());
      }
      sb.append('}');
    } else if (value instanceof Iterable<?> items) {
      sb.append('[');
      boolean first = true;
      for (Object item : items) {
        if (!first) {
          sb.append(',');
        }
        first = false;
        writeValue(sb, item);
      }
      sb.append(']');
    } else {
      writeString(sb, String.valueOf(value));
    }
  }

  private static void writeString(StringBuilder sb, String text) {
    sb.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    sb.append('"');
  }

  private static final class Parser {
    private final String text;
    private int pos;

    Parser(String text) {
      this.text = text;
    }

    Map<String, String> parseObject() {
      skipWhitespace();
      expect('{');
      Map<String, String> result = new LinkedHashMap<>();
      skipWhitespace();
      if (peek() == '}') {
        pos++;
        return finish(result);
      }
      while (true) {
        skipWhitespace();
        String key = parseString();
        skipWhitespace();
        expect(':');
        skipWhitespace();
        result.put(key, parseValue());
        skipWhitespace();
        char next = peek();
        if (next == ',') {
          pos++;
        } else if (next == '}') {
          pos++;
          return finish(result);
        } else {
          throw new IllegalArgumentException("JSON inválido en la posición " + pos);
        }
      }
    }

    private Map<String, String> finish(Map<String, String> result) {
      skipWhitespace();
      if (pos != text.length()) {
        throw new IllegalArgumentException("JSON con texto sobrante");
      }
      return result;
    }

    private String parseValue() {
      char c = peek();
      if (c == '"') {
        return parseString();
      }
      if (c == '{' || c == '[') {
        throw new IllegalArgumentException("JSON anidado no soportado");
      }
      int start = pos;
      while (pos < text.length()) {
        char x = text.charAt(pos);
        if (x == ',' || x == '}' || Character.isWhitespace(x)) {
          break;
        }
        pos++;
      }
      if (start == pos) {
        throw new IllegalArgumentException("Valor JSON vacío");
      }
      return text.substring(start, pos);
    }

    private String parseString() {
      expect('"');
      StringBuilder sb = new StringBuilder();
      while (true) {
        if (pos >= text.length()) {
          throw new IllegalArgumentException("Texto JSON sin cerrar");
        }
        char c = text.charAt(pos++);
        if (c == '"') {
          return sb.toString();
        }
        if (c != '\\') {
          sb.append(c);
          continue;
        }
        if (pos >= text.length()) {
          throw new IllegalArgumentException("Escape JSON incompleto");
        }
        char e = text.charAt(pos++);
        switch (e) {
          case '"', '\\', '/' -> sb.append(e);
          case 'b' -> sb.append('\b');
          case 'f' -> sb.append('\f');
          case 'n' -> sb.append('\n');
          case 'r' -> sb.append('\r');
          case 't' -> sb.append('\t');
          case 'u' -> {
            if (pos + 4 > text.length()) {
              throw new IllegalArgumentException("Escape unicode incompleto");
            }
            sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
            pos += 4;
          }
          default -> throw new IllegalArgumentException("Escape JSON desconocido");
        }
      }
    }

    private void skipWhitespace() {
      while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
        pos++;
      }
    }

    private char peek() {
      if (pos >= text.length()) {
        throw new IllegalArgumentException("JSON incompleto");
      }
      return text.charAt(pos);
    }

    private void expect(char c) {
      if (peek() != c) {
        throw new IllegalArgumentException("Se esperaba '" + c + "' en la posición " + pos);
      }
      pos++;
    }
  }
}
