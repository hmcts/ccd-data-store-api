package uk.gov.hmcts.ccd.data.casedetails.query;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

public interface CaseDetailsAuthorisationSecurity {

    <T> void secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata);

}
