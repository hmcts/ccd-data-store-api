package uk.gov.hmcts.ccd;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
}
