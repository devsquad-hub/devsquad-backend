package com.devsquad.shared.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NamedSqlTest {

  @Test
  void replacesNamedParametersInOccurrenceOrderWithoutTouchingPostgresCasts() {
    var parsed =
        NamedSql.parse(
            "select payload::text from events where owner_id = :owner and reviewer_id = :owner and"
                + " state = :state",
            Map.of("owner", "account", "state", "OPEN"));

    assertThat(parsed.sql())
        .isEqualTo(
            "select payload::text from events where owner_id = ? and reviewer_id = ? and state ="
                + " ?");
    assertThat(parsed.arguments()).containsExactly("account", "account", "OPEN");
  }

  @Test
  void preservesNullArgumentsForOptionalColumns() {
    Map<String, Object> parameters = new java.util.HashMap<>();
    parameters.put("detail", null);
    var parsed = NamedSql.parse("insert into events (detail) values (:detail)", parameters);

    assertThat(parsed.arguments()).containsExactly((Object) null);
  }
}
