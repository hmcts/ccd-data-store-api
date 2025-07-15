package uk.gov.hmcts.ccd.domain.service.common;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Comparator.comparingInt;
import static uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationUtils.caseHasClassificationEqualOrLowerThan;

@Service
public class SecurityClassificationServiceImpl implements SecurityClassificationService {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityClassificationServiceImpl.class);

    private final CaseDataAccessControl caseDataAccessControl;
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    public SecurityClassificationServiceImpl(CaseDataAccessControl caseDataAccessControl,
                                             @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                             final CaseDefinitionRepository caseDefinitionRepository) {
        this.caseDataAccessControl = caseDataAccessControl;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    public Optional<CaseDetails> applyClassification(CaseDetails caseDetails) {
        return applyClassification(caseDetails, false);
    }

    public Optional<CaseDetails> applyClassification(CaseDetails caseDetails, boolean create) {
        Optional<SecurityClassification> userClassificationOpt = getUserClassification(caseDetails, create);
        return userClassificationOpt
            .flatMap(securityClassification ->
                Optional.of(caseDetails).filter(caseHasClassificationEqualOrLowerThan(securityClassification))
                .map(cd -> {
                    if (cd.getDataClassification() == null) {
                        LOG.warn("No data classification for case with reference={},"
                            + " all fields removed", cd.getReference());
                        cd.setDataClassification(Maps.newHashMap());
                    }

                    // We no longer apply field level classification to case data.
                    // https://tools.hmcts.net/jira/browse/CCD-6378
                    return cd;
                }));
    }

    public List<AuditEvent> applyClassification(CaseDetails caseDetails, List<AuditEvent> events) {
        final Optional<SecurityClassification> userClassification = getUserClassification(caseDetails, false);

        if (null == events || !userClassification.isPresent()) {
            return newArrayList();
        }

        final ArrayList<AuditEvent> classifiedEvents = newArrayList();

        for (AuditEvent event : events) {
            if (userClassification.get().higherOrEqualTo(event.getSecurityClassification())) {
                classifiedEvents.add(event);
            }
        }

        return classifiedEvents;
    }

    public SecurityClassification getClassificationForEvent(CaseTypeDefinition caseTypeDefinition,
                                                            CaseEventDefinition caseEventDefinition) {
        return caseTypeDefinition
            .getEvents()
            .stream()
            .filter(e -> e.getId().equals(caseEventDefinition.getId()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("EventId %s not found", caseEventDefinition.getId())))
            .getSecurityClassification();
    }

    public Optional<SecurityClassification> getUserClassification(CaseTypeDefinition caseTypeDefinition,
                                                                  boolean isCreateProfile) {
        return maxSecurityClassification(caseDataAccessControl
            .getUserClassifications(caseTypeDefinition, isCreateProfile));
    }

    @Override
    public Optional<SecurityClassification> getUserClassification(CaseDetails caseDetails, boolean create) {
        if (create) {
            return maxSecurityClassification(caseDataAccessControl.getUserClassifications(
                caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId()), true));
        }
        return maxSecurityClassification(caseDataAccessControl.getUserClassifications(caseDetails));
    }

    private Optional<SecurityClassification> maxSecurityClassification(Set<SecurityClassification> classifications) {
        return classifications.stream()
            .filter(classification -> classification != null)
            .max(comparingInt(SecurityClassification::getRank));
    }
}
