package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
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
    public void shouldPrepareCaseTypesDefURL() {
        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", "http://foo");

        String url = applicationParams.caseTypesDefURL("caseTypesDefURL");
        assertEquals("http://foo/api/data/case-type/caseTypesDefURL", url);
    }

    @Test
    public void shouldPrepareGetCallbackRetries() {
        ReflectionTestUtils.setField(applicationParams, "callbackRetries",  Arrays.asList(1));

        List<Integer> value = applicationParams.getCallbackRetries();
        assertEquals(1, value.get(0));
    }

    @Test
    public void shouldPrepareGetElasticSearchHosts() {
        ReflectionTestUtils.setField(applicationParams, "elasticSearchHosts",  Arrays.asList("hhh"));

        List<String> value = applicationParams.getElasticSearchHosts();
        assertEquals("hhh", value.get(0));
    }

    @Test
    public void shouldPrepareRoleAssignments() {
        final var enablePseudoRoleAssignmentsGeneration = true;
        final var enablePseudoAccessProfilesGeneration = false;
        final var enableAttributeBasedAccessControl = true;
        ReflectionTestUtils.setField(applicationParams, "enablePseudoRoleAssignmentsGeneration",
            enablePseudoRoleAssignmentsGeneration);
        ReflectionTestUtils.setField(applicationParams, "enablePseudoAccessProfilesGeneration",
            enablePseudoAccessProfilesGeneration);
        ReflectionTestUtils.setField(applicationParams, "enableAttributeBasedAccessControl",
            enableAttributeBasedAccessControl);

        assertEquals(applicationParams.getEnablePseudoRoleAssignmentsGeneration(),
            enablePseudoRoleAssignmentsGeneration);
        assertEquals(applicationParams.getEnablePseudoAccessProfilesGeneration(),enablePseudoAccessProfilesGeneration);
        assertEquals(applicationParams.getEnableAttributeBasedAccessControl(),enableAttributeBasedAccessControl);
    }
}

