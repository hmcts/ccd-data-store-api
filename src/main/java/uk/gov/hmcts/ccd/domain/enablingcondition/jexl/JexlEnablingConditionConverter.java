package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

    private static final String EQUALS_CONDITION = "=";

    private static final String NOT_EQUALS_CONDITION = "!=";

    private static final String CONTAINS_CONDITION = "CONTAINS";

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
            Optional<ParsedCondition> parsedCondition = parseCondition(condition);
            if (parsedCondition.isEmpty()) {
                return Optional.empty();
            }
            parsedConditions.add(parseEqualityCondition(parsedCondition.get()));
        }
        return Optional.of(String.join(conditionalOperator, parsedConditions));
    }

    private Optional<ParsedCondition> parseCondition(String condition) {
        String normalisedCondition = stripTrailingCloseParentheses(condition);
        Optional<ParsedCondition> notEqualsCondition = parseCondition(normalisedCondition, NOT_EQUALS_CONDITION, false);
        if (notEqualsCondition.isPresent()) {
            return notEqualsCondition;
        }

        Optional<ParsedCondition> containsCondition = parseCondition(normalisedCondition, CONTAINS_CONDITION, false);
        if (containsCondition.isPresent()) {
            return containsCondition;
        }

        return parseCondition(normalisedCondition, EQUALS_CONDITION, true);
    }

    private Optional<ParsedCondition> parseCondition(String condition, String operator, boolean equality) {
        int operatorIndex = CONTAINS_CONDITION.equals(operator)
            ? findOutsideQuotes(condition, operator, 0)
            : condition.indexOf(operator);
        if (operatorIndex < 0) {
            return Optional.empty();
        }

        String leftHandSide = condition.substring(0, operatorIndex).trim();
        String rightHandValue = condition.substring(operatorIndex + operator.length()).trim();
        if (leftHandSide.isEmpty() || !isQuotedValue(rightHandValue)) {
            return Optional.empty();
        }

        return Optional.of(new ParsedCondition(leftHandSide, operator, rightHandValue, equality));
    }

    private String stripTrailingCloseParentheses(String condition) {
        String strippedCondition = condition.trim();
        while (strippedCondition.endsWith(")")) {
            strippedCondition = strippedCondition.substring(0, strippedCondition.length() - 1).trim();
        }
        return strippedCondition;
    }

    private boolean isQuotedValue(String value) {
        return value.length() >= 2
            && value.charAt(0) == '"'
            && value.charAt(value.length() - 1) == '"'
            && value.indexOf('"', 1) == value.length() - 1;
    }

    private String parseEqualityCondition(ParsedCondition condition) {
        String rightHandValue = condition.rightHandValue();
        String value = rightHandValue;
        if (rightHandValue.equals(WILD_CARD)) {
            value = WILD_CARD_VALUE;
        }
        return getLeftHandSideOfEquals(condition) + getEqualsSign(condition, rightHandValue) + value;
    }

    private String getLeftHandSideOfEquals(ParsedCondition condition) {
        String variable = condition.leftHandSide().replace("[", "");
        return variable.replace("]", "");
    }

    private String getEqualsSign(ParsedCondition condition, String value) {
        if (value.equals(WILD_CARD)) {
            return condition.equality() ? CONTAINS_OPERATOR : NOT_CONTAINS_OPERATOR;
        }

        return condition.equality()
            ? condition.operator() + condition.operator()
            : condition.operator();
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

    private record ParsedCondition(String leftHandSide, String operator, String rightHandValue, boolean equality) {
    }
}
