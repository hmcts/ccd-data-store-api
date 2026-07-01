package uk.gov.hmcts.ccd.data.definition;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import uk.gov.hmcts.ccd.AbstractBaseIntegrationTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Disabled("Fixed by Mario's PR")
public class UIDefinitionRepositoryWireMockNotRunningTest extends AbstractBaseIntegrationTest {

    @Autowired
    private UIDefinitionRepository uiDefinitionRepository;

    @Test
    public void errorGetDefinition() {
        final ServiceException
            exception =
            assertThrows(ServiceException.class,
                () -> uiDefinitionRepository.getWorkBasketResult("TestAddressBookCase"));
        assertThat(exception.getMessage(),
            startsWith(
                "Problem getting WorkBasketResult definition for case type: TestAddressBookCase because of "));
    }
}
