package uk.gov.hmcts.ccd.data.caseaccess;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CaseLinkRepository extends CrudRepository<CaseLinkEntity, CaseLinkEntity.CaseLinkPrimaryKey> {

}
