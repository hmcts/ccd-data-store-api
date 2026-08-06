package uk.gov.hmcts.ccd.data.casedetails.search.builder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SqlParamAssert {

    /**
     * Matches a named parameter reference. The trailing {@code $} is significant - access control params are
     * namespaced {@code abac$...} (see {@link GrantTypeSqlQueryBuilder#ACCESS_CONTROL_PARAM_PREFIX}) and must
     * be captured whole, not truncated at the prefix.
     */
    private static final Pattern NAMED_PARAM = Pattern.compile(":([A-Za-z_][A-Za-z0-9_$]*)");

    private SqlParamAssert() {
    }

    static Set<String> namedParametersIn(String query) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = NAMED_PARAM.matcher(query == null ? "" : query);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }

    /**
     * Asserts the params map lines up exactly with the placeholders in the query - nothing referenced but
     * unbound, nothing bound but unreferenced.
     */
    static void assertParamsMatchQuery(String query, Map<String, Object> params) {
        Set<String> placeholders = namedParametersIn(query);

        Set<String> unbound = new TreeSet<>(placeholders);
        unbound.removeAll(params.keySet());
        assertTrue(unbound.isEmpty(),
            "SQL references parameters that were never bound: " + unbound
                + "\n  query : " + query + "\n  params: " + params);

        Set<String> orphaned = new TreeSet<>(params.keySet());
        orphaned.removeAll(placeholders);
        assertTrue(orphaned.isEmpty(),
            "params map holds values the SQL never references: " + orphaned
                + "\n  query : " + query + "\n  params: " + params);
    }

    /**
     * Asserts the params map matches the query's placeholders AND holds exactly the expected key/value pairs,
     * supplied as alternating key, value arguments.
     */
    static void assertBoundParams(String query, Map<String, Object> params, Object... expectedKeyValues) {
        assertParamsMatchQuery(query, params);

        Map<String, Object> expected = new LinkedHashMap<>();
        for (int i = 0; i < expectedKeyValues.length; i += 2) {
            expected.put((String) expectedKeyValues[i], expectedKeyValues[i + 1]);
        }
        assertEquals(new TreeSet<>(expected.keySet()), new TreeSet<>(params.keySet()),
            "unexpected set of bound params\n  query : " + query);
        expected.forEach((key, value) ->
            assertEquals(value, params.get(key),
                "wrong bound value for '" + key + "'\n  params: " + params));
    }
}
