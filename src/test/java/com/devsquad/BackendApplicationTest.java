package com.devsquad;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import com.devsquad.shared.persistence.JdbcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class BackendApplicationTest {

  @Inject JdbcClient jdbc;

  @Test
  void startsAndAppliesCoreSchema() {
    var tables =
        jdbc.sql("select table_name from information_schema.tables where table_schema = 'public'")
            .query(String.class)
            .list();

    assertThat(tables).contains("accounts", "hubs", "projects", "tasks", "notifications");
  }

  @Test
  void publicHubCatalogStartsEmpty() {
    given().when().get("/api/v1/public/hubs").then().statusCode(200).body("items", empty());
  }

  @Test
  void compatibilityReadinessReflectsQuarkusHealth() {
    given()
        .when()
        .get("/actuator/health/readiness")
        .then()
        .statusCode(200)
        .body("status", equalTo("UP"));
  }

  @Test
  void privateEndpointWithoutIdentityReturnsUnauthorized() {
    given()
        .when()
        .get("/api/v1/hubs")
        .then()
        .statusCode(401)
        .body("code", equalTo("authentication_required"));
  }

  @Test
  void unknownRouteRemainsNotFound() {
    given().when().get("/api/v1/does-not-exist").then().statusCode(404);
  }

  @Test
  void invalidRequestUsesStableProblemContract() {
    given()
        .contentType("application/json")
        .body("{}")
        .when()
        .post("/api/v1/attachments/upload-ticket")
        .then()
        .statusCode(400)
        .contentType("application/problem+json")
        .body("type", equalTo("https://devsquad.app/problems/invalid_request"))
        .body("code", equalTo("invalid_request"));
  }
}
