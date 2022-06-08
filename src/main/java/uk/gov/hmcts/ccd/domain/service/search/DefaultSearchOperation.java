package uk.gov.hmcts.ccd.domain.service.search;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.List;
import java.util.Map;

@Service
@Qualifier(DefaultSearchOperation.QUALIFIER)
public class DefaultSearchOperation implements SearchOperation {
    public static final String QUALIFIER = "default";

    private final CaseDetailsRepository caseDetailsRepository;

    @Autowired
    public DefaultSearchOperation(@Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                          CaseDetailsRepository caseDetailsRepository) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    @Override
    public List<CaseDetails> execute(MetaData metaData, Map<String, String> criteria) {

        if (!metaData.validateAndConvertReference()) {
            return Lists.newArrayList();
        }
        return caseDetailsRepository.findByMetaDataAndFieldData(metaData, criteria);
    }

    @Override
    public List<CaseDetails> execute(MigrationParameters migrationParameters) {
        return caseDetailsRepository.findByParamsWithLimit(migrationParameters);
    }
}
