package com.devsquad.seed;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DemoSeedContractTest {

  @Test
  void seedAssetsExistAndAreExplicitlyTransactional() throws Exception {
    var scriptPath = Path.of("scripts/seed-demo.sql");
    var wrapperPath = Path.of("scripts/seed-demo.sh");

    assertThat(scriptPath).exists();
    assertThat(wrapperPath).exists();

    var script = Files.readString(scriptPath);
    var wrapper = Files.readString(wrapperPath);
    assertThat(script)
        .contains(
            "DEMO_SEED_V1",
            "BEGIN;",
            "COMMIT;",
            "pg_advisory_xact_lock",
            "demo_seed_namespace_collision");
    assertThat(wrapper)
        .contains("ON_ERROR_STOP=1", "SEED_PSQL_URL", "SEED_CONFIRM");
  }
}
