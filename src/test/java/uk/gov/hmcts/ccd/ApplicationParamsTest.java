package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationParamsTest {

    private ApplicationParams applicationParams = new ApplicationParams();

    @Test
    public void shouldPrepareUrlForDisplayWizardPageCollection() {
        ReflectionTestUtils.setField(applicationParams, "uiDefinitionHost", "http://foo");

        String url = applicationParams.displayWizardPageCollection("some:CaseType", "some:EventId");

        assertEquals("http://foo/api/display/wizard-page-structure/case-types/some%3ACaseType/event-triggers/some:EventId",
                     url);
    }


    @Test
    public void shouldPrepareRoleAssignments() {
        final var enablePseudoRoleAssignmentsGeneration = true;
        final var enableAttributeBasedAccessControl = true;
        ReflectionTestUtils.setField(applicationParams, "enablePseudoRoleAssignmentsGeneration",
            enablePseudoRoleAssignmentsGeneration);
        ReflectionTestUtils.setField(applicationParams, "enableAttributeBasedAccessControl",
            enableAttributeBasedAccessControl);

        assertEquals(applicationParams.getEnablePseudoRoleAssignmentsGeneration(),
            enablePseudoRoleAssignmentsGeneration);
        assertEquals(applicationParams.getEnableAttributeBasedAccessControl(),enableAttributeBasedAccessControl);
    }

    @Test
    public void shouldPrepareElasticsearchSetup() {
        final var elasticSearchDataHosts = List.of("host1", "host2", "host3");
        final var elasticsearchNodeDiscoveryEnabled = true;
        final var elasticsearchNodeDiscoveryFrequencyMillis = 1L;
        final var elasticsearchNodeDiscoveryFilter = "filter";
        ReflectionTestUtils.setField(applicationParams, "elasticSearchDataHosts", elasticSearchDataHosts);
        ReflectionTestUtils.setField(applicationParams,
            "elasticsearchNodeDiscoveryEnabled", elasticsearchNodeDiscoveryEnabled);
        ReflectionTestUtils.setField(applicationParams,
            "elasticsearchNodeDiscoveryFrequencyMillis", elasticsearchNodeDiscoveryFrequencyMillis);
        ReflectionTestUtils.setField(applicationParams,
            "elasticsearchNodeDiscoveryFilter", elasticsearchNodeDiscoveryFilter);

        assertEquals(elasticSearchDataHosts, applicationParams.getElasticSearchDataHosts());
        assertEquals(elasticsearchNodeDiscoveryEnabled, applicationParams.isElasticsearchNodeDiscoveryEnabled());
        assertEquals(elasticsearchNodeDiscoveryFrequencyMillis,
            applicationParams.getElasticsearchNodeDiscoveryFrequencyMillis());
        assertEquals(elasticsearchNodeDiscoveryFilter, applicationParams.getElasticsearchNodeDiscoveryFilter());
    }
}

