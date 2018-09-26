package uk.gov.hmcts.ccd.domain.service.search.filter;

import java.util.Optional;

public interface CaseFilterFactory<T> {

    Optional<T> create(String caseTypeId);

}
