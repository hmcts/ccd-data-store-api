package uk.gov.hmcts.ccd.domain.service.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControl;

class AttributeBasedAccessControlServiceTest {

    private AttributeBasedAccessControlService attributeBasedAccessControlService;

    @Mock
    private CompoundAccessControlService compoundAccessControlService;

    @Mock
    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;

    @Mock
    private CaseDefinitionRepository caseDefinitionRepository;

    @BeforeEach
    void setUp() {
        attributeBasedAccessControlService = new AttributeBasedAccessControlService(compoundAccessControlService,
            defaultCaseDataAccessControl,
            caseDefinitionRepository);
    }

    @Test
    void canAccessCaseTypeWithCriteria() {
    }

    @Test
    void canAccessCaseStateWithCriteria() {
    }

    @Test
    void canAccessCaseEventWithCriteria() {
    }

    @Test
    void canAccessCaseFieldsWithCriteria() {
    }

    @Test
    void canAccessCaseViewFieldWithCriteria() {
    }

    @Test
    void canAccessCaseFieldsForUpsert() {
    }

    @Test
    void setReadOnlyOnCaseViewFieldsIfNoAccess() {
    }
}
