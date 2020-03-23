package uk.gov.hmcts.ccd.data.definition;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

public class CaseDefinitionRepositoryWireMockNotRunningTest extends BaseTest {

    @Autowired
    private CaseDefinitionRepository caseDefinitionRepository;

    @Test
    public void shouldFailToGetCaseTypesForJurisdiction() {
        final ServiceException
            exception =
            assertThrows(ServiceException.class,
                         () -> caseDefinitionRepository.getCaseTypesForJurisdiction("nor_defined"));
        assertThat(exception.getMessage(),
                   startsWith("Problem getting case types for the Jurisdiction:nor_defined because of "));
    }

    @Test
    public void shouldFailToGetCaseType() {
        final ServiceException
            exception =
            assertThrows(ServiceException.class, () -> caseDefinitionRepository.getCaseType("anything"));
        assertThat(exception.getMessage(), startsWith("Problem getting case type definition for anything because of "));
    }

    @Test
    public void shouldFailToGetBaseTypes() {
        when(caseDefinitionRepository.getBaseTypes()).thenCallRealMethod();
        final ServiceException
            exception =
            assertThrows(ServiceException.class, () -> caseDefinitionRepository.getBaseTypes());
        assertThat(exception.getMessage(),
                   startsWith("Problem getting base types definition from definition store because of "));
    }

    @Test
    public void shouldFailToGetClassificationsForUserRoleList() {
        List<String> userRoles = Arrays.asList("neither_defined", "nor_defined");
        final ServiceException
            exception =
            assertThrows(ServiceException.class,
                () -> caseDefinitionRepository.getClassificationsForUserRoleList(userRoles));
        assertThat(exception.getMessage(),
                   startsWith("Error while retrieving classification for user roles " + userRoles + " because of "));
    }
}
