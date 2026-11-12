package com.example.prices;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.prices.domain.DomainArchitectureViolationFixture;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchitectureTest {

  private static final ArchRule DOMAIN_MUST_NOT_DEPEND_ON_FRAMEWORKS =
      noClasses()
          .that()
          .resideInAPackage("com.example.prices.domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta.persistence..",
              "com.fasterxml.jackson..",
              "tools.jackson..");

  private static final ArchRule APPLICATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE =
      noClasses()
          .that()
          .resideInAPackage("com.example.prices.application..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("com.example.prices.infrastructure..");

  private static final ArchRule PRICE_LAYERS_MUST_NOT_HAVE_CYCLES =
      SlicesRuleDefinition.slices().matching("com.example.prices.(*)..").should().beFreeOfCycles();

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importProductionClasses() throws URISyntaxException {
    Path productionClassesPath =
        Path.of(
            PricesServiceApplication.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
    productionClasses = new ClassFileImporter().importPath(productionClassesPath);
  }

  @Test
  void domainDoesNotDependOnFrameworks() {
    DOMAIN_MUST_NOT_DEPEND_ON_FRAMEWORKS.check(productionClasses);
  }

  @Test
  void applicationDoesNotDependOnInfrastructure() {
    APPLICATION_MUST_NOT_DEPEND_ON_INFRASTRUCTURE.check(productionClasses);
  }

  @Test
  void priceLayersHaveNoCycles() {
    PRICE_LAYERS_MUST_NOT_HAVE_CYCLES.check(productionClasses);
  }

  @Test
  void domainRuleDetectsAFrameworkDependencyInAControlledFixture() {
    JavaClasses fixtureClasses =
        new ClassFileImporter().importClasses(DomainArchitectureViolationFixture.class);

    EvaluationResult result = DOMAIN_MUST_NOT_DEPEND_ON_FRAMEWORKS.evaluate(fixtureClasses);

    assertThat(result.hasViolation()).isTrue();
    assertThat(result.getFailureReport().getDetails())
        .anyMatch(detail -> detail.contains(DomainArchitectureViolationFixture.class.getName()));
  }
}
