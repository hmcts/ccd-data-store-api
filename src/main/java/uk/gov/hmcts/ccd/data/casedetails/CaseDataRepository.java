package uk.gov.hmcts.ccd.data.casedetails;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseDataRepository extends CrudRepository<CaseDetailsEntity, Long> {

}
