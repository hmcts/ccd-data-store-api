package uk.gov.hmcts.ccd.fta.steps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapVerifier {

    public static final String DONT_CARE = "[[DONT_CARE]]";
    public static final String NONE_NULL = "[[NONE_NULL]]";

    // TODO: Compare 2 maps and return any differences they may have as a list of
    // Strings, each String to describe a particular difference.
    // If the Maps are all equal, then the return an empty list.
    public static MapVerificationResult verifyMap(Map<String, Object> expectedMap, Map<String, Object> actualMap,
            int maxMessageDepth) {
        if (maxMessageDepth < 0)
            throw new IllegalArgumentException("Max depth cannot be negative.");
        return verifyMap("actualResponse", expectedMap, actualMap, 0, maxMessageDepth);
    }

    private static MapVerificationResult verifyMap(String fieldPrefix, Map<String, Object> expectedMap,
            Map<String, Object> actualMap, int currentDepth, int maxMessageDepth) {

        boolean shouldReportAnyDifference = currentDepth <= maxMessageDepth;

        if (expectedMap == actualMap) {
            return MapVerificationResult.DEFAULT_VERIFIED;
        } else if (expectedMap == null) {
            return new MapVerificationResult(fieldPrefix, false,
                    shouldReportAnyDifference ? "Map is expected to be null, but is actuall not." : null, currentDepth,  maxMessageDepth);
        } else if (actualMap == null) {
            return new MapVerificationResult(fieldPrefix, false,
                    shouldReportAnyDifference ? "Map is expected to be non-null, but is actuall null." : null,
                    currentDepth, maxMessageDepth);
        }

        List<String> unexpectedFields = checkForUnenexpectedlyAvailableFields(expectedMap, actualMap);
        List<String> unavailableFields = checkForUnenexpectedlyUnavailableFields(expectedMap, actualMap);
        List<String> badValueMessages = collectBadValueMessages(expectedMap, actualMap, fieldPrefix, currentDepth,
                maxMessageDepth);
        List<MapVerificationResult> badSubmaps = collectBadSubmaps(expectedMap, actualMap, fieldPrefix,
                currentDepth, maxMessageDepth);

        if (unexpectedFields.size() == 0 && unavailableFields.size() == 0 && badValueMessages.size() == 0
                && badSubmaps.size() == 0) {
            return MapVerificationResult.minimalVerifiedResult(fieldPrefix, currentDepth, maxMessageDepth);
        }
        return new MapVerificationResult(fieldPrefix, false, null, unexpectedFields,
                    unavailableFields,
                    badValueMessages, badSubmaps, currentDepth, maxMessageDepth);
    }

    @SuppressWarnings("unchecked")
    private static List<MapVerificationResult> collectBadSubmaps(Map<String, Object> expectedMap,
            Map<String, Object> actualMap, String fieldPrefix, int currentDepth, int maxMessageDepth) {
        ArrayList<MapVerificationResult> differences = new ArrayList<>();
        expectedMap.keySet().stream().filter(keyOfExpected -> actualMap.containsKey(keyOfExpected))
                .forEach(commonKey -> {
                    Object expectedValue = expectedMap.get(commonKey);
                    Object actualValue = actualMap.get(commonKey);
                    if (expectedValue instanceof Map && actualValue instanceof Map) {
                        MapVerificationResult subresult = verifyMap(fieldPrefix + "." + commonKey,
                                (Map<String, Object>) expectedValue,
                                (Map<String, Object>) actualValue, currentDepth + 1, maxMessageDepth);
                        if (!subresult.isVerified()) {
                            differences.add(subresult);
                        }
                    }
                });
        return differences;
    }

    private static List<String> checkForUnenexpectedlyAvailableFields(Map<String, Object> expectedMap,
            Map<String, Object> actualMap) {
        return actualMap.keySet().stream().filter(keyOfActual -> !expectedMap.containsKey(keyOfActual))
                .collect(Collectors.toList());
    }

    private static List<String> checkForUnenexpectedlyUnavailableFields(Map<String, Object> expectedMap,
            Map<String, Object> actualMap) {
        return expectedMap.keySet().stream().filter(keyOfExpected -> !actualMap.containsKey(keyOfExpected)
                && isExpectedToBeAvaiableInActual(expectedMap.get(keyOfExpected))).collect(Collectors.toList());
    }

    private static List<String> collectBadValueMessages(Map<String, Object> expectedMap, Map<String, Object> actualMap,
            String fieldPrefix, int currentDepth, int maxMessageDepth) {
        List<String> badValueMessages = new ArrayList<>();
        expectedMap.keySet().stream().filter(keyOfExpected -> actualMap.containsKey(keyOfExpected))
                .forEach(commonKey -> {
                    Object expectedValue = expectedMap.get(commonKey);
                    Object actualValue = actualMap.get(commonKey);
                    if (expectedValue == actualValue) {
                        //
                    } else if (expectedValue == null) {
                        badValueMessages.add("Must be null: " + commonKey);
                    } else if (actualValue == null) {
                        badValueMessages.add("Must not be null: " + commonKey);
                    } else if (!(expectedValue instanceof Map && actualValue instanceof Map)) {
                        Object outcome = compareValues(fieldPrefix, commonKey, expectedValue, actualValue,
                                currentDepth, maxMessageDepth);
                        if (!Boolean.TRUE.equals(outcome)) {
                            if (outcome instanceof String)
                                badValueMessages.add((String) outcome);
                            else
                                badValueMessages.add(fieldPrefix + "." + commonKey);
                        }
                    }
        });
        return badValueMessages;
    }

    private static Object compareValues(String fieldPrefix, String commonKey, Object expectedValue,
            Object actualValue, int currentDepth, int maxMessageDepth) {
        boolean justCompare = currentDepth > maxMessageDepth;
        if (expectedValue instanceof String && DONT_CARE.equalsIgnoreCase((String) expectedValue)) {
            return Boolean.TRUE;
        }
        else if (expectedValue == actualValue) {
            return Boolean.TRUE;
        }
        else if (expectedValue == null) {
            return justCompare ? Boolean.FALSE : "Must be null: " + commonKey;
        } else if (actualValue == null) {
            return justCompare ? canAcceptNullFor(expectedValue) : "Must be non-null: " + commonKey;
        }else {
            return compareNonNullLiteral(commonKey, expectedValue, actualValue, justCompare);
        }
    }

    private static Object compareNonNullLiteral(String fieldName, Object expectedValue, Object actualValue,
            boolean justCompare) {
        if (expectedValue.equals(actualValue))
            return Boolean.TRUE;
        return justCompare ? Boolean.FALSE
                : fieldName + ": expected '" + expectedValue + "' but got '" + actualValue + "'";
    }

    private static boolean isExpectedToBeAvaiableInActual(Object expectedValue) {
        if (expectedValue instanceof String)
            return !DONT_CARE.equalsIgnoreCase((String) expectedValue);
        return true;
    }

    private static Boolean canAcceptNullFor(Object expectedValue) {
        if (!(expectedValue instanceof String)) {
            return Boolean.FALSE;
        }
        return !NONE_NULL.equalsIgnoreCase((String) expectedValue);
    }

}
