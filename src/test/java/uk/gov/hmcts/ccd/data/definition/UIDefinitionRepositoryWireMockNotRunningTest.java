package uk.gov.hmcts.ccd.data.definition;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@org.junit.Ignore("Fixed by Mario's PR")
public class UIDefinitionRepositoryWireMockNotRunningTest extends BaseTest {

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
