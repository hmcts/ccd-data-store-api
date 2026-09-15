package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the deployment plumbing for the {@code token.claim.validation.enabled} feature flag (CCD-4311).
 *
 * <p>The flag reaches the deployed data-store pod only through a chart's {@code environment:} block. Setting
 * {@code env.TOKEN_CLAIM_VALIDATION_ENABLED} in a Jenkinsfile defines it on the Jenkins agent instead, which has no
 * effect on the pod - that was the original defect, and it let the BEFTA suites run green against a pod where the
 * flag was still off, so the functional evidence for the claim-validation fix did not actually exist.
 *
 * <p>The base {@code values.yaml} is deliberately excluded: it feeds the long-lived AAT and production deployments,
 * where the flag stays off until it is switched on as a release step.
 */
class TokenClaimValidationFlagChartConfigurationTest {

    private static final String FLAG = "TOKEN_CLAIM_VALIDATION_ENABLED";
    private static final Path CHART_DIR = Path.of("charts/ccd-data-store-api");
    private static final Path BASE_VALUES = CHART_DIR.resolve("values.yaml");

    @SuppressWarnings("unchecked")
    private static Map<String, Object> javaEnvironment(final Path valuesFile) throws IOException {
        final Map<String, Object> values = new Yaml().load(Files.readString(valuesFile));
        final Map<String, Object> java = (Map<String, Object>) values.get("java");
        assertThat(java).as("%s must declare a java section", valuesFile).isNotNull();

        final Map<String, Object> environment = (Map<String, Object>) java.get("environment");
        assertThat(environment).as("%s must declare java.environment", valuesFile).isNotNull();
        return environment;
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"values.preview.template.yaml", "values.aat.template.yaml"})
    @DisplayName("the test environments deploy with claim validation switched on")
    void testEnvironmentsEnableTheFlag(final String valuesFileName) throws IOException {
        final Map<String, Object> environment = javaEnvironment(CHART_DIR.resolve(valuesFileName));

        assertThat(environment)
            .as("%s must pass %s to the container, otherwise BEFTA exercises the flag's disabled path",
                valuesFileName, FLAG)
            .containsKey(FLAG);
        assertThat(String.valueOf(environment.get(FLAG))).isEqualTo("true");
    }

    @Test
    @DisplayName("the base values keep the flag off, so it stays a deliberate release step in AAT and production")
    void baseValuesDoNotEnableTheFlag() throws IOException {
        assertThat(javaEnvironment(BASE_VALUES))
            .as("enabling %s in values.yaml would switch the fix on in production without a release decision", FLAG)
            .doesNotContainKey(FLAG);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"Jenkinsfile_CNP", "Jenkinsfile_nightly"})
    @DisplayName("the charts are the only place the flag is configured")
    void jenkinsfilesDoNotExportTheFlag(final String jenkinsfile) throws IOException {
        // An `env.` export defines the variable on the Jenkins agent, not in the deployed container, so it has no
        // effect on the data-store pod. Both Jenkinsfiles used to set it, which made the flag look wired up when it
        // was not. Keeping the chart templates as the single source of truth avoids re-creating that illusion.
        assertThat(Files.readString(Path.of(jenkinsfile)))
            .as("%s must not re-introduce a no-op agent-level export of %s", jenkinsfile, FLAG)
            .doesNotContain(FLAG);
    }
}
