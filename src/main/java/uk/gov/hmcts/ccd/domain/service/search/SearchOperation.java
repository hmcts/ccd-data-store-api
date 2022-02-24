package uk.gov.hmcts.ccd.domain.service.search;

import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;

public interface SearchOperation {

    List<CaseDetails> execute(MetaData metaData, Map<String, String> criteria);

    List<CaseDetails> execute(MigrationParameters migrationParameters);

}
