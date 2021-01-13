package uk.gov.hmcts.ccd.domain.enablingcondition.jexl;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    private static final String AND_CONDITION_REGEX = "\\sAND\\s(?=(([^\"]*\"){2})*[^\"]*$)";

    private static final String OR_CONDITION_REGEX = "\\sOR\\s(?=(([^\"]*\"){2})*[^\"]*$)";

    private static final String AND_OPERATOR = " and ";

    private static final String OR_OPERATOR = " or ";

    private static final Pattern EQUALITY_CONDITION_PATTERN =
        Pattern.compile("\\s*?(.*)\\s*?(=|CONTAINS)\\s*?(\".*\")\\s*?");

    private static final Pattern NOT_EQUAL_CONDITION_PATTERN =
        Pattern.compile("\\s*?(.*)\\s*?(!=|CONTAINS)\\s*?(\".*\")\\s*?");

    private Pattern orConditionPattern = Pattern.compile(OR_CONDITION_REGEX);

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
            String[] conditions;
            Matcher matcher = orConditionPattern.matcher(enablingCondition);
            if (matcher.find()) {
                conditions = enablingCondition.split(OR_CONDITION_REGEX);
                conditionalOperator = OR_OPERATOR;
            } else {
                conditions = enablingCondition.split(AND_CONDITION_REGEX);
            }
            return Optional.of(buildEnablingCondition(conditions, conditionalOperator));
        }
        return Optional.empty();
    }

    private String buildEnablingCondition(String[] conditions, String conditionalOperator) {
        List<String> parsedConditions = new LinkedList<>();
        for (String condition : conditions) {
            Matcher equalityMatcher = EQUALITY_CONDITION_PATTERN.matcher(condition);
            Matcher notEqualityMatcher = NOT_EQUAL_CONDITION_PATTERN.matcher(condition);
            if (notEqualityMatcher.find()) {
                parsedConditions.add(parseEqualityCondition(notEqualityMatcher, false));
            } else if (equalityMatcher.find()) {
                parsedConditions.add(parseEqualityCondition(equalityMatcher, true));
            }
        }
        return parsedConditions
            .stream()
            .collect(Collectors.joining(conditionalOperator));
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
        return matcher.group(1).trim();
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
}
