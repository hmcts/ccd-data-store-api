package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.enums.GrantType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Security regression tests for {@link GrantTypeSqlQueryBuilder} (exercised through the concrete
 * {@link BasicGrantTypeQueryBuilder}, which inherits {@code createQuery} unchanged).
 * These tests pin the FIXED behaviour: the previously concatenated attribute values are now
 * bound as named parameters, and the caseAccessCategory LIKE pattern escapes its wildcards. Each
 * test asserts the exact SQL fragment shape the builder now emits AND the value that travels in
 * the params map, so a regression to string concatenation (or to a fixed, colliding param name)
 * fails here.
 */
class GrantTypeSqlQueryBuilderSecurityTest extends GrantTypeQueryBuilderTest {

    private static final String QUOTE_PAYLOAD = "x' OR '1'='1";

    private BasicGrantTypeQueryBuilder queryBuilder;

    @Mock
    private AccessControlService accessControlService;

    @Mock
    private CaseDataAccessControl caseDataAccessControl;

    @Mock
    private CaseTypeDefinition caseTypeDefinition;

    @Mock
    private ApplicationParams applicationParams;

    private AutoCloseable openMocks;

    @BeforeEach
    void setUp() {
        openMocks = MockitoAnnotations.openMocks(this);
        queryBuilder = new BasicGrantTypeQueryBuilder(accessControlService, caseDataAccessControl,
            applicationParams);
        CaseStateDefinition caseStateDefinition = mock(CaseStateDefinition.class);
        when(caseStateDefinition.getId()).thenReturn("CaseCreated");
        when(accessControlService
            .filterCaseStatesByAccess(anyList(), anySet(), any(Predicate.class)))
            .thenReturn(Lists.newArrayList(caseStateDefinition));
    }

    @AfterEach
    void tearDown() throws Exception {
        openMocks.close();
    }

    @Test
    @DisplayName("jurisdiction is bound as a named parameter, not concatenated into the SQL")
    void jurisdiction_isBound_notConcatenated() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", QUOTE_PAYLOAD, "", "", null);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains(GrantTypeSqlQueryBuilder.JURISDICTION + " = :jurisdiction_1_basic"),
            "expected a bound placeholder, but was: " + query);
        assertFalse(query.contains(QUOTE_PAYLOAD),
            "payload must not appear in SQL text, but was: " + query);
        assertEquals(QUOTE_PAYLOAD, params.get("jurisdiction_1_basic"),
            "expected the payload to be bound under the suffixed key, params were: " + params);
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "jurisdiction_1_basic", QUOTE_PAYLOAD);
    }

    @Test
    @DisplayName("region is bound as a named parameter, not concatenated into the SQL")
    void region_isBound_notConcatenated() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", QUOTE_PAYLOAD, null);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains(GrantTypeSqlQueryBuilder.REGION + " = :region_1_basic"),
            "expected a bound placeholder, but was: " + query);
        assertFalse(query.contains(QUOTE_PAYLOAD),
            "payload must not appear in SQL text, but was: " + query);
        assertEquals(QUOTE_PAYLOAD, params.get("region_1_basic"),
            "expected the payload to be bound under the suffixed key, params were: " + params);
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "region_1_basic", QUOTE_PAYLOAD);
    }

    @Test
    @DisplayName("location is bound as a named parameter, not concatenated into the SQL")
    void location_isBound_notConcatenated() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", QUOTE_PAYLOAD, "", null);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains(GrantTypeSqlQueryBuilder.LOCATION + " = :location_1_basic"),
            "expected a bound placeholder, but was: " + query);
        assertFalse(query.contains(QUOTE_PAYLOAD),
            "payload must not appear in SQL text, but was: " + query);
        assertEquals(QUOTE_PAYLOAD, params.get("location_1_basic"),
            "expected the payload to be bound under the suffixed key, params were: " + params);
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "location_1_basic", QUOTE_PAYLOAD);
    }

    @Test
    @DisplayName("caseAccessGroupId is bound into the JSONB containment, not concatenated")
    void caseAccessGroupId_isBound_notConcatenated() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", "", null, null, QUOTE_PAYLOAD);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains("data->'CaseAccessGroups' @> jsonb_build_array(jsonb_build_object("
                + "'value', jsonb_build_object('caseAccessGroupId', CAST(:case_access_group_id_1_basic "
                + "AS text))))"),
            "expected a bound JSONB containment, but was: " + query);
        assertFalse(query.contains(QUOTE_PAYLOAD),
            "payload must not appear in SQL text, but was: " + query);
        assertEquals(QUOTE_PAYLOAD, params.get("case_access_group_id_1_basic"),
            "expected the payload to be bound under the suffixed key, params were: " + params);
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "case_access_group_id_1_basic", QUOTE_PAYLOAD);
    }

    @Test
    @DisplayName("case reference with a quote is bound, never concatenated")
    void reference_singleQuote_isBound_notInSqlText() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", "", null, QUOTE_PAYLOAD);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains("reference in (:references_1_basic)"),
            "expected a bound placeholder, but was: " + query);
        assertFalse(query.contains(QUOTE_PAYLOAD),
            "payload must not appear in SQL text, but was: " + query);
        assertTrue(paramsContainValue(params),
            "expected the payload to be bound as a parameter, params were: " + params);
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "references_1_basic", List.of(QUOTE_PAYLOAD));
    }

    @Test
    @DisplayName("caseAccessCategory binds the value and escapes % and _ so they are not wildcards")
    void caseAccessCategory_isBound_andWildcardsEscaped() {
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PUBLIC", "", "", "", null);

        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getAccessProfile()).thenReturn("ROLE1");
        when(accessProfile.getCaseAccessCategories()).thenReturn("a%b_c");
        when(caseDataAccessControl.filteredAccessProfiles(anyList(), any(CaseTypeDefinition.class),
            anyBoolean())).thenReturn(Sets.newHashSet(accessProfile));

        Map<String, Object> params = Maps.newHashMap();
        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment),
            params, caseTypeDefinition);

        assertTrue(query.contains(GrantTypeSqlQueryBuilder.CASE_ACCESS_CATEGORY
                + " LIKE :case_access_category_1_1_basic ESCAPE '\\'"),
            "expected a bound LIKE pattern with an ESCAPE clause, but was: " + query);
        assertFalse(query.contains("LIKE 'a%b_c%'"),
            "the raw wildcard literal must no longer be concatenated, but was: " + query);
        assertEquals("a\\%b\\_c%", params.get("case_access_category_1_1_basic"),
            "expected the input wildcards escaped in the bound value, params were: " + params);
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC"),
            "case_access_category_1_1_basic", "a\\%b\\_c%");
    }

    @Test
    @DisplayName("caseAccessCategory LIKE always carries an explicit ESCAPE '\\' clause")
    void caseAccessCategory_emitsExplicitEscapeClause() {
        // The ESCAPE '\' clause is behaviourally invisible on Postgres, whose LIKE already uses
        // backslash as the DEFAULT escape character, so no integration test running against Postgres
        // can fail when it is removed. This string-level assertion is the only regression guard for
        // the clause: it keeps the escaping explicit rather than relying on an engine default, which
        // matters for SQL-standard portability (e.g. Oracle has no default LIKE escape character).
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PUBLIC", "", "", "", null);

        AccessProfile accessProfile = mock(AccessProfile.class);
        when(accessProfile.getAccessProfile()).thenReturn("ROLE1");
        when(accessProfile.getCaseAccessCategories()).thenReturn("CIVIL");
        when(caseDataAccessControl.filteredAccessProfiles(anyList(), any(CaseTypeDefinition.class),
            anyBoolean())).thenReturn(Sets.newHashSet(accessProfile));

        Map<String, Object> params = Maps.newHashMap();
        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment),
            params, caseTypeDefinition);

        assertTrue(query.contains(GrantTypeSqlQueryBuilder.CASE_ACCESS_CATEGORY
                + " LIKE :case_access_category_1_1_basic ESCAPE '\\'"),
            "expected the LIKE pattern to keep its explicit ESCAPE '\\' clause, but was: " + query);
    }

    @Test
    @DisplayName("two OR-joined groups bind the same attribute under distinct keys, no overwrite")
    void twoGroups_bindSameAttribute_underDistinctKeys_noOverwrite() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment groupOne = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "JUR-A", "", "REGION-A", null);
        RoleAssignment groupTwo = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "JUR-B", "", "REGION-B", null);

        String query = queryBuilder.createQuery(Lists.newArrayList(groupOne, groupTwo), params,
            caseTypeDefinition);

        assertTrue(query.contains(":region_1_basic") && query.contains(":region_2_basic"),
            "expected two distinct region placeholders, but was: " + query);
        assertNotEquals("region_1_basic", "region_2_basic");

        assertTrue(params.containsKey("region_1_basic") && params.containsKey("region_2_basic"),
            "expected both suffixed region keys, params were: " + params);
        assertEquals(Sets.newHashSet("REGION-A", "REGION-B"),
            Sets.newHashSet(params.get("region_1_basic"), params.get("region_2_basic")),
            "both region values must be retrievable under distinct keys, params were: " + params);

        assertTrue(query.contains(":jurisdiction_1_basic") && query.contains(":jurisdiction_2_basic"),
            "expected two distinct jurisdiction placeholders, but was: " + query);
        assertEquals(Sets.newHashSet("JUR-A", "JUR-B"),
            Sets.newHashSet(params.get("jurisdiction_1_basic"), params.get("jurisdiction_2_basic")),
            "both jurisdiction values must be retrievable under distinct keys, params were: " + params);

        assertEquals(8, params.size(), "unexpected number of bound params, params were: " + params);
        assertEquals(Set.of("CaseCreated"), params.get("states_1_basic"));
        assertEquals(Set.of("CaseCreated"), params.get("states_2_basic"));
        assertEquals(List.of("PUBLIC", "PRIVATE"), params.get("classifications_1_basic"));
        assertEquals(List.of("PUBLIC", "PRIVATE"), params.get("classifications_2_basic"));
    }

    @Test
    @DisplayName("jurisdiction emits '<jurisdiction column> = :param' with the value bound")
    void jurisdiction_emitsEqualsBind_withRealColumn() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "PROBATE", "", "", null);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains("jurisdiction = :jurisdiction_1_basic"),
            "expected the real jurisdiction column bound, but was: " + query);
        assertEquals("PROBATE", params.get("jurisdiction_1_basic"));
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "jurisdiction_1_basic", "PROBATE");
    }

    @Test
    @DisplayName("region emits the real JSON path column '= :param' with the value bound")
    void region_emitsEqualsBind_withRealColumn() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", "GB-EAST", null);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains("data #>> '{caseManagementLocation,region}' = :region_1_basic"),
            "expected the real region column bound, but was: " + query);
        assertEquals("GB-EAST", params.get("region_1_basic"));
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "region_1_basic", "GB-EAST");
    }

    @Test
    @DisplayName("location emits the real JSON path column '= :param' with the value bound")
    void location_emitsEqualsBind_withRealColumn() {
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "LOC-1", "", null);

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains("data #>> '{caseManagementLocation,baseLocation}' = :location_1_basic"),
            "expected the real location column bound, but was: " + query);
        assertEquals("LOC-1", params.get("location_1_basic"));
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "location_1_basic", "LOC-1");
    }

    @Test
    @DisplayName("caseAccessGroupId emits jsonb_build containment around the bound value")
    void caseAccessGroupId_emitsJsonbContainmentBind() {
        when(applicationParams.getCaseGroupAccessFilteringEnabled()).thenReturn(true);
        Map<String, Object> params = Maps.newHashMap();
        RoleAssignment roleAssignment = createRoleAssignment(GrantType.BASIC, "CASE", "ROLE1",
            "PRIVATE", "", "", "", null,
            null, "GRP-1");

        String query = queryBuilder.createQuery(Lists.newArrayList(roleAssignment), params,
            caseTypeDefinition);

        assertTrue(query.contains("data->'CaseAccessGroups' @> jsonb_build_array(jsonb_build_object("
                + "'value', jsonb_build_object('caseAccessGroupId', CAST(:case_access_group_id_1_basic "
                + "AS text))))"),
            "expected jsonb_build containment around the bound value, but was: " + query);
        assertEquals("GRP-1", params.get("case_access_group_id_1_basic"));
        assertParamsExactly(params,
            "states_1_basic", Set.of("CaseCreated"),
            "classifications_1_basic", List.of("PUBLIC", "PRIVATE"),
            "case_access_group_id_1_basic", "GRP-1");
    }

    private boolean paramsContainValue(Map<String, Object> params) {
        return params.values().stream().anyMatch(paramValue ->
            paramValue instanceof Collection
                ? ((Collection<?>) paramValue).contains(GrantTypeSqlQueryBuilderSecurityTest.QUOTE_PAYLOAD)
                : GrantTypeSqlQueryBuilderSecurityTest.QUOTE_PAYLOAD.equals(paramValue));
    }

    private void assertParamsExactly(Map<String, Object> params, Object... expectedKeyValues) {
        assertEquals(expectedKeyValues.length / 2, params.size(),
            "unexpected number of bound params, params were: " + params);
        for (int i = 0; i < expectedKeyValues.length; i += 2) {
            String key = (String) expectedKeyValues[i];
            assertEquals(expectedKeyValues[i + 1], params.get(key),
                "wrong bound value for '" + key + "', params were: " + params);
        }
    }
}
