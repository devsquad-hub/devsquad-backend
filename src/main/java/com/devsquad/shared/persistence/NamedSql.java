package com.devsquad.shared.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

record NamedSql(String sql, List<Object> arguments) {

  static NamedSql parse(String source, Map<String, Object> parameters) {
    var sql = new StringBuilder(source.length());
    var arguments = new ArrayList<>();
    var quoted = false;

    for (var index = 0; index < source.length(); index++) {
      var character = source.charAt(index);
      if (character == '\'') {
        quoted = !quoted;
        sql.append(character);
        continue;
      }
      if (!quoted
          && character == ':'
          && (index == 0 || source.charAt(index - 1) != ':')
          && index + 1 < source.length()
          && Character.isJavaIdentifierStart(source.charAt(index + 1))) {
        var end = index + 2;
        while (end < source.length() && Character.isJavaIdentifierPart(source.charAt(end))) end++;
        var name = source.substring(index + 1, end);
        if (!parameters.containsKey(name))
          throw new IllegalArgumentException("Missing SQL parameter: " + name);
        sql.append('?');
        arguments.add(parameters.get(name));
        index = end - 1;
        continue;
      }
      sql.append(character);
    }
    return new NamedSql(sql.toString(), java.util.Collections.unmodifiableList(arguments));
  }
}
