package uk.gov.hmcts.ccd.data.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;

import javax.inject.Inject;
import java.util.List;

@Service
public class UserService {

    private UserRepository userRepository;
    private CaseDefinitionRepository caseDefinitionRepository;
    private JurisdictionMapper jurisdictionMapper;

    @Inject
    public UserService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository,
                       JurisdictionMapper jurisdictionMapper) {
        this.userRepository = userRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.jurisdictionMapper = jurisdictionMapper;
    }

    public UserProfile getUserProfile() {

        IDAMProperties idamProperties = userRepository.getUserDetails();
        String userId = idamProperties.getEmail();
        UserDefault userDefault = userRepository.getUserDefaultSettings(userId);
        List<String> jurisdictionsId = userDefault.getJurisdictionsId();

        List<Jurisdiction> jurisdictionsDefinition = caseDefinitionRepository.getJurisdictions(jurisdictionsId);

        return createUserProfile(idamProperties, userDefault, jurisdictionsDefinition);
    }

    private UserProfile createUserProfile(IDAMProperties idamProperties, UserDefault userDefault, List<Jurisdiction> jurisdictionsDefinition) {

        JurisdictionDisplayProperties[] resultJurisdictions = toResponse(jurisdictionsDefinition);

        WorkbasketDefault workbasketDefault = new WorkbasketDefault();
        workbasketDefault.setJurisdictionId(userDefault.getWorkBasketDefaultJurisdiction());
        workbasketDefault.setCaseTypeId(userDefault.getWorkBasketDefaultCaseType());
        workbasketDefault.setStateId(userDefault.getWorkBasketDefaultState());

        UserProfile userProfile = new UserProfile();
        userProfile.setJurisdictions(resultJurisdictions);
        userProfile.getUser().setIdamProperties(idamProperties);
        userProfile.getDefaultSettings().setWorkbasketDefault(workbasketDefault);

        return userProfile;
    }

    private JurisdictionDisplayProperties[] toResponse(List<Jurisdiction> jurisdictionsDefinition) {
        return jurisdictionsDefinition.stream().map(jurisdictionMapper::toResponse)
        .toArray(JurisdictionDisplayProperties[]::new);
    }
}
