package uk.gov.hmcts.ccd.domain.enablingcondition;

/**
 * Convert enabling condition to specific format based on the requirements.
 * Sub-Classes should implement this interface and provide the logic specif to their implementation.
 */
public interface EnablingConditionConverter {
    String convert(String enablingCondition);
}
