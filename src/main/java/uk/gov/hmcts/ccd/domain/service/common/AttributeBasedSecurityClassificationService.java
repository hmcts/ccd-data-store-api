package uk.gov.hmcts.ccd.domain.service.common;

import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.CaseDataAccessControl;

import static java.util.Comparator.comparingInt;

@Service
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class AttributeBasedSecurityClassificationService
    extends SecurityClassificationServiceImpl
    implements AccessControl {


    private CaseDataAccessControl caseDataAccessControl;

    @Autowired
    public AttributeBasedSecurityClassificationService(@Qualifier(CachedUserRepository.QUALIFIER)
                                                               UserRepository userRepository,
                                                       CaseDataAccessControl caseDataAccessControl) {
        super(userRepository);
        this.caseDataAccessControl = caseDataAccessControl;
    }


    @Override
    public Optional<SecurityClassification> getUserClassification(CaseDetails caseDetails, boolean create) {
        Set<AccessProfile> accessProfiles;
        if (create) {
            accessProfiles = caseDataAccessControl.generateAccessProfilesByCaseTypeId(caseDetails.getCaseTypeId());
        } else {
            accessProfiles = caseDataAccessControl
                .generateAccessProfilesByCaseReference(caseDetails.getReferenceAsString());
        }
        return getMaxSecurityClassification(accessProfiles);
    }

    private Optional<SecurityClassification> getMaxSecurityClassification(Set<AccessProfile> accessProfiles) {
        if (accessProfiles != null) {
            return accessProfiles.stream()
                .map(accessProfile -> SecurityClassification.valueOf(accessProfile.getSecurityClassification()))
                .max(comparingInt(SecurityClassification::getRank));
        }
        return Optional.empty();
    }

}
