package com.devsquad;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.devsquad", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  @ArchTest
  static final ArchRule production_code_has_no_spring_dependencies =
      noClasses().should().dependOnClassesThat().resideInAnyPackage("org.springframework..");

  @ArchTest
  static final ArchRule domain_is_framework_independent =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "org.hibernate..",
              "software.amazon.awssdk..");

  @ArchTest
  static final ArchRule domain_does_not_depend_on_adapters =
      noClasses()
          .that()
          .resideInAPackage("..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..adapter..", "..application..");

  @ArchTest
  static final ArchRule application_does_not_depend_on_infrastructure =
      noClasses()
          .that()
          .resideInAPackage("..application..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..adapter..", "software.amazon.awssdk..", "com.fasterxml.jackson..");

  @ArchTest
  static final ArchRule inbound_adapters_do_not_access_persistence =
      noClasses()
          .that()
          .resideInAPackage("..adapter.in..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..adapter.out.persistence..", "..shared.persistence..");
}
