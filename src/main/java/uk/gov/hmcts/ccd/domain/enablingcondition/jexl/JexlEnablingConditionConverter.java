package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.jexl3.JexlOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.enablingcondition.EnablingConditionConverter;

/**
 * This class converts the post state enabling condition to Jexl condition.
 * For now we are handling AND -> and
 * OR -> or
 * = -> ==
 * FieldA="*" -> FieldA=~".*" (content checking)
 * FieldA!="*" -> FieldA!~".*" (content checking)
 */
@Component
@Qualifier("jexl")
public class JexlEnablingConditionConverter implements EnablingConditionConverter {

    private static final String AND_CONDITION = "AND";

    private static final String OR_CONDITION = "OR";

    private static final String AND_OPERATOR = " and ";

    private static final String OR_OPERATOR = " or ";

    private static final Pattern EQUALITY_CONDITION_PATTERN =
        Pattern.compile("\\s*([^!=]+?)\\s*(=|CONTAINS)\\s*(\"[^\"]*\")\\s*\\)*\\s*");

    private static final Pattern NOT_EQUAL_CONDITION_PATTERN =
        Pattern.compile("\\s*([^!=]+?)\\s*(!=|CONTAINS)\\s*(\"[^\"]*\")\\s*\\)*\\s*");

    private static final String WILD_CARD = "\"*\"";

    private static final String WILD_CARD_VALUE = "\".*\"";

    private static final String CONTAINS_OPERATOR = JexlOperator.CONTAINS.getOperatorSymbol();

    private static final String NOT_CONTAINS_OPERATOR = "!~";

    @Override
    public String convert(String enablingCondition) {
        Optional<String> parsedCondition = parseEnablingCondition(enablingCondition);
        return parsedCondition.orElse(enablingCondition);
    }

    private Optional<String> parseEnablingCondition(String enablingCondition) {
        if (enablingCondition != null) {
            String conditionalOperator = AND_OPERATOR;
            List<String> conditions;
            if (containsOutsideQuotes(enablingCondition, OR_CONDITION)) {
                conditions = splitOutsideQuotes(enablingCondition, OR_CONDITION);
                conditionalOperator = OR_OPERATOR;
            } else {
                conditions = splitOutsideQuotes(enablingCondition, AND_CONDITION);
            }
            return buildEnablingCondition(conditions, conditionalOperator);
        }
        return Optional.empty();
    }

    private Optional<String> buildEnablingCondition(List<String> conditions, String conditionalOperator) {
        List<String> parsedConditions = new ArrayList<>();
        for (String condition : conditions) {
            Matcher equalityMatcher = EQUALITY_CONDITION_PATTERN.matcher(condition);
            Matcher notEqualityMatcher = NOT_EQUAL_CONDITION_PATTERN.matcher(condition);
            if (notEqualityMatcher.matches()) {
                parsedConditions.add(parseEqualityCondition(notEqualityMatcher, false));
            } else if (equalityMatcher.matches()) {
                parsedConditions.add(parseEqualityCondition(equalityMatcher, true));
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(String.join(conditionalOperator, parsedConditions));
    }

    private String parseEqualityCondition(Matcher matcher, boolean equality) {
        String rightHandValue = getRightHandSideOfEquals(matcher);
        String value = rightHandValue;
        if (rightHandValue.equals(WILD_CARD)) {
            value = WILD_CARD_VALUE;
        }
        return getLeftHandSideOfEquals(matcher) + getEqualsSign(matcher, rightHandValue, equality) + value;
    }

    private String getLeftHandSideOfEquals(Matcher matcher) {
        String variable = matcher.group(1).trim().replace("[", "");
        return variable.replace("]", "");
    }

    private String getEqualsSign(Matcher matcher, String value, boolean equality) {
        if (value.equals(WILD_CARD)) {
            return equality ? CONTAINS_OPERATOR : NOT_CONTAINS_OPERATOR;
        }

        return equality
            ? matcher.group(2).trim() + "" + matcher.group(2).trim()
            : matcher.group(2).trim();
    }

    private String getRightHandSideOfEquals(Matcher matcher) {
        return matcher.group(3).trim();
    }

    private boolean containsOutsideQuotes(String value, String operator) {
        return findOutsideQuotes(value, operator, 0) >= 0;
    }

    private List<String> splitOutsideQuotes(String value, String operator) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int operatorIndex = findOutsideQuotes(value, operator, start);
        while (operatorIndex >= 0) {
            parts.add(value.substring(start, operatorIndex));
            start = operatorIndex + operator.length();
            operatorIndex = findOutsideQuotes(value, operator, start);
        }
        parts.add(value.substring(start));
        return parts;
    }

    private int findOutsideQuotes(String value, String operator, int fromIndex) {
        boolean inQuotes = false;
        for (int i = 0; i <= value.length() - operator.length(); i++) {
            if (value.charAt(i) == '"') {
                inQuotes = !inQuotes;
            }
            if (i >= fromIndex && !inQuotes && value.startsWith(operator, i)
                && isBoundedByWhitespace(value, i, operator.length())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBoundedByWhitespace(String value, int operatorIndex, int operatorLength) {
        return operatorIndex > 0
            && Character.isWhitespace(value.charAt(operatorIndex - 1))
            && operatorIndex + operatorLength < value.length()
            && Character.isWhitespace(value.charAt(operatorIndex + operatorLength));
    }
}
