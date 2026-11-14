package spacare.web;

import java.util.*;

/** JSON codec for API objects; no third-party runtime beyond H2. */
public final class Json {

  public static String stringify(Object value) {
    if (value == null) return "null";
    if (
      value instanceof Number || value instanceof Boolean
    ) return value.toString();
    if (value instanceof Map<?, ?> m) {
      var a = new ArrayList<String>();
      m.forEach((k, v) -> a.add(stringify(k.toString()) + ":" + stringify(v)));
      return "{" + String.join(",", a) + "}";
    }
    if (value instanceof Iterable<?> list) {
      var a = new ArrayList<String>();
      list.forEach(v -> a.add(stringify(v)));
      return "[" + String.join(",", a) + "]";
    }
    String s = value.toString();
    var out = new StringBuilder("\"");
    for (char c : s.toCharArray())
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 32) out.append(String.format("\\u%04x", (int) c));
          else out.append(c);
        }
      }
    return out.append('"').toString();
  }

  public static Object parse(String source) {
    var p = new Parser(source);
    Object v = p.value();
    p.ws();
    if (p.i != source.length()) throw new IllegalArgumentException(
      "JSON không hợp lệ"
    );
    return v;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, Object> object(String s) {
    Object v = parse(s);
    if (!(v instanceof Map)) throw new IllegalArgumentException(
      "Yêu cầu JSON object"
    );
    return (Map<String, Object>) v;
  }

  private static class Parser {

    final String s;
    int i, depth;

    Parser(String s) {
      this.s = s;
    }

    void ws() {
      while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    char take() {
      if (i >= s.length()) throw new IllegalArgumentException(
        "JSON chưa đầy đủ"
      );
      return s.charAt(i++);
    }

    Object value() {
      ws();
      if (++depth > 40) throw new IllegalArgumentException("JSON quá sâu");
      try {
        if (i >= s.length()) throw new IllegalArgumentException("Thiếu JSON");
        char c = s.charAt(i);
        if (c == '"') return str();
        if (c == '{') {
          i++;
          var m = new LinkedHashMap<String, Object>();
          ws();
          if (i < s.length() && s.charAt(i) == '}') {
            i++;
            return m;
          }
          do {
            ws();
            String k = str();
            ws();
            if (take() != ':') throw new IllegalArgumentException(
              "Thiếu dấu :"
            );
            m.put(k, value());
            ws();
            c = take();
            if (c == '}') return m;
            if (c != ',') throw new IllegalArgumentException("Thiếu dấu ,");
          } while (true);
        }
        if (c == '[') {
          i++;
          var a = new ArrayList<Object>();
          ws();
          if (i < s.length() && s.charAt(i) == ']') {
            i++;
            return a;
          }
          do {
            a.add(value());
            ws();
            c = take();
            if (c == ']') return a;
            if (c != ',') throw new IllegalArgumentException("Thiếu dấu ,");
          } while (true);
        }
        for (String word : List.of("true", "false", "null"))
          if (s.startsWith(word, i)) {
            i += word.length();
            return word.equals("null") ? null : Boolean.valueOf(word);
          }
        int start = i;
        while (i < s.length() && "-+0123456789.eE".indexOf(s.charAt(i)) >= 0)
          i++;
        if (start == i) throw new IllegalArgumentException("JSON không hợp lệ");
        String n = s.substring(start, i);
        try {
          if (
            n.contains(".") || n.contains("e") || n.contains("E")
          ) return Double.parseDouble(n);
          return Long.parseLong(n);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Số không hợp lệ");
        }
      } finally {
        depth--;
      }
    }

    String str() {
      if (take() != '"') throw new IllegalArgumentException("Thiếu chuỗi JSON");
      var out = new StringBuilder();
      while (true) {
        char c = take();
        if (c == '"') return out.toString();
        if (c < 32) throw new IllegalArgumentException(
          "Ký tự JSON không hợp lệ"
        );
        if (c == '\\') {
          char e = take();
          switch (e) {
            case '"', '\\', '/' -> out.append(e);
            case 'n' -> out.append('\n');
            case 'r' -> out.append('\r');
            case 't' -> out.append('\t');
            case 'b' -> out.append('\b');
            case 'f' -> out.append('\f');
            case 'u' -> {
              if (i + 4 > s.length()) throw new IllegalArgumentException(
                "Unicode không hợp lệ"
              );
              out.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
              i += 4;
            }
            default -> throw new IllegalArgumentException(
              "Escape không hợp lệ"
            );
          }
        } else out.append(c);
      }
    }
  }
}
