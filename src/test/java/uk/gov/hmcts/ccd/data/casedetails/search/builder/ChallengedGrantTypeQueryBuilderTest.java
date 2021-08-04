package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ChallengedGrantTypeQueryBuilderTest extends GrantTypeQueryBuilderTest {

    private ChallengedGrantTypeQueryBuilder challengedGrantTypeQueryBuilder;

    @BeforeEach
    void setUp() {
        challengedGrantTypeQueryBuilder = new ChallengedGrantTypeQueryBuilder();
    }

    @Test
    void shouldNotReturnQueryWhenChallengedGrantTypeNotPresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.STANDARD, "CASE",
            "PRIVATE", "", "", null);
        String query = challengedGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap());

        assertNotNull(query);
        assertEquals("", query);
    }

    @Test
    void shouldReturnQueryWhenChallengedGrantTypePresentInRoleAssignments() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.CHALLENGED, "CASE",
            "PRIVATE",  "TEST", "", "", null);
        String query = challengedGrantTypeQueryBuilder
            .createQuery(Lists.newArrayList(roleAssignment), Maps.newHashMap());

        assertNotNull(query);
        String expectedValue = "( security_classification in (:classifications) AND jurisdiction in (:jurisdictions) )";
        assertEquals(expectedValue, query);
    }
}
