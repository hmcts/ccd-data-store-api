package uk.gov.hmcts.ccd.data.casedetails;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.domain.model.std.SupplementaryData;

@Named
@Qualifier("default")
@Singleton
public class DefaultSupplementaryDataRepository implements SupplementaryDataRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSupplementaryDataRepository.class);

    @PersistenceContext
    private EntityManager em;


    @Override
    public SupplementaryData upsert(final String caseId, final SupplementaryData supplementaryData) {
        LOG.debug("Insert or update supplementary data");
        // @Todo enable this once supplementary data last modifed column is added!!
//        newCaseDetailsEntity.setSupplementaryDataLastModified(LocalDateTime.now(ZoneOffset.UTC));

        return null;
    }
}
