package uk.gov.hmcts.ccd.data.casedetails.search;

import java.time.LocalDate;

import jakarta.persistence.Query;

public class DateRangeMetaDataCriterion extends MetaDataCriterion {

    private static final String FROM_SUFFIX = "_from";
    private static final String TO_SUFFIX = "_to";

    private final LocalDate soughtDate;

    public DateRangeMetaDataCriterion(String field, LocalDate soughtDate) {
        super(field, soughtDate.toString());
        this.soughtDate = soughtDate;
    }

    @Override
    public String buildClauseString(String operation) {
        String parameterId = buildParameterId();
        return getField() + " >= " + PARAM_PREFIX + parameterId + FROM_SUFFIX
            + " AND " + getField() + " < " + PARAM_PREFIX + parameterId + TO_SUFFIX;
    }

    @Override
    public void bindParameters(Query query) {
        String parameterId = buildParameterId();
        query.setParameter(parameterId + FROM_SUFFIX, soughtDate.atStartOfDay());
        query.setParameter(parameterId + TO_SUFFIX, soughtDate.plusDays(1).atStartOfDay());
    }
}