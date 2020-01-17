package uk.gov.hmcts.ccd.data.definition;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.WorkbasketInputDefinition;

public class HttpUIDefinitionGatewayTest extends WireMockBaseTest {

    private static final int VERSION = 33;

    @Inject
    private HttpUIDefinitionGateway httpUIDefinitionGateway;

    @Test
    public void getDefinition() {
        final SearchResult workBasketResult = httpUIDefinitionGateway.getWorkBasketResult(VERSION,"TestAddressBookCase");
        assertThat(workBasketResult.getFields().length, is(3));
    }

    @Test
    @DisplayName("should Return Workbasket Input Definitions")
    public void shouldReturnWorkbasketInputDefinitions() {
        final WorkbasketInputDefinition workbasketInputDefinitions = httpUIDefinitionGateway
            .getWorkbasketInputDefinitions(VERSION, "TestAddressBookCase");
        assertThat(workbasketInputDefinitions.getFields().size(), is(3));
    }
}
