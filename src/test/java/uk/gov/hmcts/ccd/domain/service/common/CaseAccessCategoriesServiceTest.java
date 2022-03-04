package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaseAccessCategoriesServiceTest {

    private CaseAccessCategoriesService caseAccessCategoriesService;

    @BeforeEach
    void setUp() {
        caseAccessCategoriesService = new CaseAccessCategoriesService();
    }

    @Test
    void shouldReturnTrueWhenCreateCase() {
        AccessProfile accessProfile = mock(AccessProfile.class);
        Predicate<CaseDetails> caseDetailsPredicate = caseAccessCategoriesService
            .caseHasMatchingCaseAccessCategories(Sets.newHashSet(accessProfile), true);
        CaseDetails caseDetails = new CaseDetails();
        assertTrue(caseDetailsPredicate.test(caseDetails));
    }

    @Test
    void shouldReturnFalseWhenAccessProfilesAreEmpty() {
        Predicate<CaseDetails> caseDetailsPredicate = caseAccessCategoriesService
            .caseHasMatchingCaseAccessCategories(Sets.newHashSet(), false);
        CaseDetails caseDetails = new CaseDetails();
        assertFalse(caseDetailsPredicate.test(caseDetails));
    }

    @Test
    void shouldReturnFalseWhenCaseAccessCategoryValueInCaseDataIsEmpty() {
        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getCaseAccessCategories()).thenReturn("Civil/Standard,Criminal/Serious");
        Predicate<CaseDetails> caseDetailsPredicate = caseAccessCategoriesService
            .caseHasMatchingCaseAccessCategories(Sets.newHashSet(accessProfile), false);
        assertFalse(caseDetailsPredicate.test(createCaseDetails("")));
    }

    @Test
    void shouldReturnFalseWhenCaseAccessCategoryValueNotMatchWithAccessProfileValue() {
        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getCaseAccessCategories()).thenReturn("Civil/Standard,Criminal/Serious");
        Predicate<CaseDetails> caseDetailsPredicate = caseAccessCategoriesService
            .caseHasMatchingCaseAccessCategories(Sets.newHashSet(accessProfile), false);
        assertFalse(caseDetailsPredicate.test(createCaseDetails("Civil/Legal/Test")));
    }

    @Test
    void shouldReturnTrueWhenCaseAccessCategoryValueStartWithAccessProfileValue() {
        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getCaseAccessCategories()).thenReturn("Civil/Standard,Criminal/Serious");
        Predicate<CaseDetails> caseDetailsPredicate = caseAccessCategoriesService
            .caseHasMatchingCaseAccessCategories(Sets.newHashSet(accessProfile), false);
        assertTrue(caseDetailsPredicate.test(createCaseDetails("Civil/Standard/Test")));
    }

    private CaseDetails createCaseDetails(String caseAccessCategoryValue) {
        JsonNode caseAccessCategory = new TextNode(caseAccessCategoryValue);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("CaseAccessCategory", caseAccessCategory);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setData(data);
        return caseDetails;
    }
}
