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
    public static List<String> verifyMap(Map<String, Object> expectedMap, Map<String, Object> actualMap,
            int maxMessageDepth) {
        if (maxMessageDepth < 0)
            throw new IllegalArgumentException("Max depth cannot be negative.");
        return verifyMap("actualResponse", expectedMap, actualMap, 0, maxMessageDepth);
    }

    private static List<String> verifyMap(String fieldPrefix, Map<String, Object> expectedMap,
            Map<String, Object> actualMap, int currentDepth, int maxMessageDepth) {

        boolean shouldReportAnyDifference = currentDepth <= maxMessageDepth;
        boolean shouldReportOnlySummary = currentDepth == maxMessageDepth;

        ArrayList<String> differences = new ArrayList<>();
        if (expectedMap == actualMap) {
            return differences;
        } else if (expectedMap == null) {
            differences.add("Map is expected to be null, but is actuall not.");
            return differences;
        } else if (actualMap == null) {
            differences.add("Map is expected to be non-null, but is actuall null.");
            return differences;
        }

        List<String> unexpectedFields = checkForUnenexpectedlyAvailableFields(expectedMap, actualMap);
        List<String> unavailableFields = checkForUnenexpectedlyUnavailableFields(expectedMap, actualMap);
        List<String> badValueMessages = collectBadValueMessages(expectedMap, actualMap, fieldPrefix, currentDepth,
                maxMessageDepth);

        if (shouldReportAnyDifference) {
            reportUnexpectedlyAvailables(unexpectedFields, differences, fieldPrefix, shouldReportOnlySummary);
            reportUnexpectedlyUnavailables(unavailableFields, differences, fieldPrefix, shouldReportOnlySummary);
            reportBadValues(badValueMessages, differences, fieldPrefix, shouldReportOnlySummary);
        }
        return differences;
    }

    private static List<String> checkForUnenexpectedlyAvailableFields(Map<String, Object> expectedMap,
            Map<String, Object> actualMap) {
        return actualMap.keySet().stream().filter(keyOfActual -> !expectedMap.containsKey(keyOfActual))
                .collect(Collectors.toList());
    }

    private static void reportUnexpectedlyAvailables(List<String> unexpectedFields, ArrayList<String> differences,
            String fieldPrefix, boolean shouldReportOnlySummary) {
        for (String unexpectedField : unexpectedFields) {
            if (!shouldReportOnlySummary) {
                    String message = (fieldPrefix + "." + unexpectedField)
                            + " is unexpected. This may be an undesirable information exposure!";
                    differences.add(message);
                }
        }
        if (unexpectedFields.size() > 0 && shouldReportOnlySummary) {
            String message = fieldPrefix + " has unexpected field(s): " + unexpectedFields
                    + " This may be an undesirable information exposure!";
            differences.add(message);
        }
    }

    private static List<String> checkForUnenexpectedlyUnavailableFields(Map<String, Object> expectedMap,
            Map<String, Object> actualMap) {
        return expectedMap.keySet().stream().filter(keyOfExpected -> !actualMap.containsKey(keyOfExpected)
                && isExpectedToBeAvaiableInActual(expectedMap.get(keyOfExpected))).collect(Collectors.toList());
    }

    private static void reportUnexpectedlyUnavailables(List<String> unavailableFields,
            ArrayList<String> differences, String fieldPrefix, boolean shouldReportOnlySummary) {
        for (String unavailableField : unavailableFields) {
            if (!shouldReportOnlySummary) {
                String message = (fieldPrefix + "." + unavailableField)
                        + " is unavaiable though it was expected to be there";
                differences.add(message);
            }
        }
        if (unavailableFields.size() > 0 && shouldReportOnlySummary) {
            String message = fieldPrefix + " lacks " + unavailableFields
                    + " field(s) that was/were actually expected to be there.";
            differences.add(message);
        }
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
                    }
                    else {
                       if(expectedValue instanceof Map && actualValue instanceof Map) {
                            @SuppressWarnings("unchecked")
                            List<String> messagesFromSubmap = collectBadValueMessages(
                                    (Map<String, Object>) expectedValue,
                                    (Map<String, Object>) actualValue,
                                    fieldPrefix + "." + commonKey, currentDepth + 1, maxMessageDepth);
                            badValueMessages.addAll(messagesFromSubmap);
                       }
                        Object outcome = compareValues(fieldPrefix, commonKey, expectedValue, actualValue,
                                currentDepth, maxMessageDepth);
                        if (outcome instanceof String)
                            badValueMessages.add((String) outcome);
                    }

        });
        return badValueMessages;
    }

    private static void reportBadValues(List<String> badValueMessages, ArrayList<String> differences,
            String fieldPrefix, boolean shouldReportOnlySummary) {
        for (String badValueMessage : badValueMessages) {
            if (!shouldReportOnlySummary) {
                String message = fieldPrefix + " contains a bad value: " + badValueMessage;
                differences.add(message);
            }
        }
        if (badValueMessages.size() > 0 && shouldReportOnlySummary) {
            String message = fieldPrefix + " contains " + badValueMessages.size() + " bad value(s): "
                    + badValueMessages;
            differences.add(message);
        }

    }

    private static Object compareValues(String fieldPrefix, String commonKey, Object expectedValue,
            Object actualValue, int currentDepth, int maxMessageDepth) {
        boolean justCompare = currentDepth <= maxMessageDepth;
        if (expectedValue == actualValue) {
            return justCompare ? Boolean.TRUE : null;
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
            return justCompare ? Boolean.TRUE : null;
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
