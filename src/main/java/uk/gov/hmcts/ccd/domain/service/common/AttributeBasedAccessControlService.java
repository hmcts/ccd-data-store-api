package uk.gov.hmcts.ccd.domain.service.common;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.service.AccessControl;


@Service
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class AttributeBasedAccessControlService extends AccessControlServiceImpl implements AccessControl {

    @Autowired
    public AttributeBasedAccessControlService(final CompoundAccessControlService compoundAccessControlService) {
        super(compoundAccessControlService);
    }

    @Override
    public boolean hasAccessControlList(Set<AccessProfile> accessProfiles,
                                        Predicate<AccessControlList> criteria,
                                        List<AccessControlList> accessControlLists) {
        List<AccessControlList> newAccessControlList = applyReadOnly(accessProfiles, accessControlLists);
        return hasAccessControlList(accessProfiles, criteria, newAccessControlList);
    }

    private List<AccessControlList> applyReadOnly(Set<AccessProfile> accessProfiles,
                                                  List<AccessControlList> accessControlLists) {
        return accessProfiles
            .stream()
            .map(accessProfile ->  updateAccessControlCRUD(accessProfile, accessControlLists))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private List<AccessControlList> updateAccessControlCRUD(AccessProfile accessProfile,
                                                                List<AccessControlList> accessControlLists) {
        return accessControlLists
            .stream()
            .filter(acls -> accessProfile.getAccessProfile().equals(acls.getAccessProfile()))
            .map(acls -> {
                AccessControlList accessControl = acls;
                if (accessProfile.getReadOnly()) {
                    accessControl = acls.duplicate();
                    accessControl.setCreate(false);
                    accessControl.setDelete(false);
                    accessControl.setUpdate(false);
                    accessControl.setRead(true);
                }
                return accessControl;
            }).collect(Collectors.toList());
    }
}
