package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;

@Component
public class CaseDetailsQueryBuilderFactory {

    public CaseDetailsQueryBuilder<CaseDetailsEntity> select(EntityManager em) {
        return new SelectCaseDetailsQueryBuilder(em);
    }

    public CaseDetailsQueryBuilder<Long> count(EntityManager em) {
        return new CountCaseDetailsQueryBuilder(em);
    }

}
