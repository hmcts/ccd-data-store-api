package uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory;
import uk.gov.hmcts.ccd.security.idam.IdamRepository;

import java.util.List;
import java.util.regex.Pattern;

import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_CITIZEN;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_JUDICIAL;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_PROFESSIONAL;
import static uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleCategory.ROLE_CATEGORY_STAFF;

@Service
public class RoleAssignmentCategoryService {

    private static final Pattern PROFESSIONAL_ROLE =
        Pattern.compile(".+-solicitor$|^caseworker-.+-localAuthority$", Pattern.CASE_INSENSITIVE);
    private static final Pattern CITIZEN_ROLE =
        Pattern.compile("^citizen(-.*)?$|^letter-holder$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JUDICIAL_ROLE = Pattern.compile(".+-panelmember$",
        Pattern.CASE_INSENSITIVE);

    private final IdamRepository idamRepository;

    public RoleAssignmentCategoryService(IdamRepository idamRepository) {
        this.idamRepository = idamRepository;
    }

    public RoleCategory getRoleCategory(String userId) {
        List<String> idamUserRoles = idamRepository.getUserRoles(userId);
        if (hasProfessionalRole(idamUserRoles)) {
            return ROLE_CATEGORY_PROFESSIONAL;
        } else if (hasCitizenRole(idamUserRoles)) {
            return ROLE_CATEGORY_CITIZEN;
        } else if (hasJudicialRole(idamUserRoles)) {
            return ROLE_CATEGORY_JUDICIAL;
        } else {
            return ROLE_CATEGORY_STAFF;
        }
    }

    private boolean hasProfessionalRole(List<String> roles) {
        return roles.stream().anyMatch(role -> PROFESSIONAL_ROLE.matcher(role).matches());
    }

    private boolean hasCitizenRole(List<String> roles) {
        return roles.stream().anyMatch(role -> CITIZEN_ROLE.matcher(role).matches());
    }

    private boolean hasJudicialRole(List<String> roles) {
        return roles.stream().anyMatch(role -> JUDICIAL_ROLE.matcher(role).matches());
    }
}
