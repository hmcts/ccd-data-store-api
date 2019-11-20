package uk.gov.hmcts.ccd.fta.steps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MapVerifier {

    private static final String ANY = "[[ANY]]";
    private static final String NOT_NULL = "[[NOT_NULL]]";

    private MapVerifier() {}

    public static MapVerificationResult verifyMap(Map<String, Object> expectedMap, Map<String, Object> actualMap,
            int maxMessageDepth) {
        return verifyMap("actualResponse.body", expectedMap, actualMap, maxMessageDepth);
    }

    public static MapVerificationResult verifyMap(String fieldPrefix, Map<String, Object> expectedMap,
            Map<String, Object> actualMap,
            int maxMessageDepth) {
        if (maxMessageDepth < 0) {
            throw new IllegalArgumentException("Max depth cannot be negative.");
        }
        return verifyMap(fieldPrefix, expectedMap, actualMap, 0, maxMessageDepth);
    }

    private static MapVerificationResult verifyMap(String fieldPrefix, Map<String, Object> expectedMap,
            Map<String, Object> actualMap, int currentDepth, int maxMessageDepth) {

        boolean shouldReportAnyDifference = currentDepth <= maxMessageDepth;

        if (expectedMap == actualMap) {
            return MapVerificationResult.DEFAULT_VERIFIED;
        } else if (expectedMap == null) {
            return new MapVerificationResult(fieldPrefix, false,
                shouldReportAnyDifference ? "Map is expected to be null, but is actually not." : null, currentDepth,  maxMessageDepth);
        } else if (actualMap == null) {
            return new MapVerificationResult(fieldPrefix, false,
                    shouldReportAnyDifference ? "Map is expected to be non-null, but is actually null." : null,
                    currentDepth, maxMessageDepth);
        }

        List<String> unexpectedFields = checkForUnexpectedlyAvailableFields(expectedMap, actualMap);
        List<String> unavailableFields = checkForUnexpectedlyUnavailableFields(expectedMap, actualMap);
        List<String> badValueMessages = collectBadValueMessagesFromMap(expectedMap, actualMap, fieldPrefix,
                currentDepth,
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

    private static List<String> checkForUnexpectedlyAvailableFields(Map<String, Object> expectedMap,
                                                                    Map<String, Object> actualMap) {
        return actualMap.keySet().stream().filter(keyOfActual -> !expectedMap.containsKey(keyOfActual))
                .collect(Collectors.toList());
    }

    private static List<String> checkForUnexpectedlyUnavailableFields(Map<String, Object> expectedMap,
                                                                      Map<String, Object> actualMap) {
        return expectedMap.keySet().stream().filter(keyOfExpected -> !actualMap.containsKey(keyOfExpected)
                && isExpectedToBeAvailableInActual(expectedMap.get(keyOfExpected))).collect(Collectors.toList());
    }

    private static List<String> collectBadValueMessagesFromMap(Map<String, Object> expectedMap,
            Map<String, Object> actualMap,
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
                        if (expectedValue instanceof Collection<?> && actualValue instanceof Collection<?>) {
                            collectBadValueMessagesFromCollection(fieldPrefix + "." + commonKey, commonKey,
                                    (Collection<?>) expectedValue,
                                    (Collection<?>) actualValue, currentDepth, maxMessageDepth, badValueMessages);
                        } else {
                            Object outcome = compareValues(fieldPrefix, commonKey, expectedValue, actualValue,
                                currentDepth, maxMessageDepth);
                            if (!Boolean.TRUE.equals(outcome)) {
                                if (outcome instanceof String) {
                                    badValueMessages.add((String) outcome);
                                } else {
                                    badValueMessages.add(fieldPrefix + "." + commonKey);
                                }
                            }
                        }
                    }
                });
        return badValueMessages;
    }

    @SuppressWarnings("unchecked")
    private static void collectBadValueMessagesFromCollection(String fieldPrefix, String field,
            Collection<?> expectedCollection,
            Collection<?> actualCollection, int currentDepth, int maxMessageDepth, List<String> badValueMessages) {
        Iterator<?> e1 = expectedCollection.iterator();
        Iterator<?> e2 = actualCollection.iterator();
        int i = 0;
        if (expectedCollection.size() != actualCollection.size()) {
            badValueMessages.add(fieldPrefix + " has unexpected number of elements. Expected: "
                    + expectedCollection.size() + ", but actual: " + actualCollection.size() + ".");
        }
        while (e1.hasNext() && e2.hasNext()) {
            Object o1 = e1.next();
            Object o2 = e2.next();
            String subfield = field + "[" + i + "]";
            if (o1 instanceof Map && o2 instanceof Map) {
                MapVerificationResult subresult = verifyMap(subfield, (Map<String, Object>) o1,
                        (Map<String, Object>) o2,
                        currentDepth + 1, maxMessageDepth);
                if (!subresult.isVerified()) {
                    badValueMessages.addAll(subresult.getAllIssues());
                }
            } else if (o1 instanceof Collection && o2 instanceof Collection) {
                collectBadValueMessagesFromCollection(fieldPrefix, subfield, (Collection<?>) o1, (Collection<?>) o2,
                        currentDepth + 1, maxMessageDepth, badValueMessages);
            } else {
                Object outcome = compareValues(subfield, subfield, o1, o2, currentDepth + 1,
                        maxMessageDepth);
                if (!Boolean.TRUE.equals(outcome)) {
                    if (outcome instanceof String) {
                        badValueMessages.add((String) outcome);
                    } else {
                        badValueMessages.add(subfield);
                    }
                }

            }
            i++;
        }
    }

    private static Object compareValues(String fieldPrefix, String commonKey, Object expectedValue,
            Object actualValue, int currentDepth, int maxMessageDepth) {
        boolean justCompare = currentDepth > maxMessageDepth;
        if (expectedValue instanceof String && ANY.equalsIgnoreCase((String) expectedValue)) {
            return Boolean.TRUE;
        } else if (expectedValue == actualValue) {
            return Boolean.TRUE;
        } else if (expectedValue == null) {
            return justCompare ? Boolean.FALSE : "Must be null: " + commonKey;
        } else if (actualValue == null) {
            return justCompare ? canAcceptNullFor(expectedValue) : "Must be non-null: " + commonKey;
        } else {
            return compareNonNullLiteral(commonKey, expectedValue, actualValue, justCompare);
        }
    }

    private static Object compareNonNullLiteral(String fieldName, Object expectedValue, Object actualValue,
            boolean justCompare) {
        if (expectedValue.equals(actualValue)) {
            return Boolean.TRUE;
        }
        return justCompare ? Boolean.FALSE
                : fieldName + ": expected '" + expectedValue + "' but got '" + actualValue + "'";
    }

    private static boolean isExpectedToBeAvailableInActual(Object expectedValue) {
        if (expectedValue instanceof String) {
            return !ANY.equalsIgnoreCase((String) expectedValue);
        }
        return true;
    }

    private static Boolean canAcceptNullFor(Object expectedValue) {
        if (!(expectedValue instanceof String)) {
            return Boolean.FALSE;
        }
        return !NOT_NULL.equalsIgnoreCase((String) expectedValue);
    }
}
