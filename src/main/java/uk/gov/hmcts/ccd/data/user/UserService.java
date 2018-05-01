package uk.gov.hmcts.ccd.data.user;

import static java.util.stream.Collectors.*;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.aggregated.WorkbasketDefault;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;

@Service
public class UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

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
        List<Jurisdiction> jurisdictionsDefinition = caseDefinitionRepository.getAllJurisdictions();

        return createUserProfile(idamProperties, userDefault, jurisdictionsDefinition);
    }

    private UserProfile createUserProfile(IDAMProperties idamProperties, UserDefault userDefault, List<Jurisdiction> jurisdictionsDefinition) {

        List<String> jurisdictionsId = userDefault.getJurisdictionsId();
        List<Jurisdiction> userJurisdictionsDefinition = extractDefinitions(jurisdictionsId, jurisdictionsDefinition);
        JurisdictionDisplayProperties[] resultJurisdictions = toResponse(userJurisdictionsDefinition);

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

    private List<Jurisdiction> extractDefinitions(List<String> jurisdictionsId, List<Jurisdiction> jurisdictionsDefinition) {
        return jurisdictionsId.stream().map(id -> {
            Optional<Jurisdiction> definition = jurisdictionsDefinition.stream().filter(def -> def.getId().equals(id)).findAny();
            if (!definition.isPresent()) {
                LOG.warn("Could not retrieve definition of jurisdiction '{}'", id);
            }
            return definition;
        }).filter(Optional::isPresent).map(Optional::get).collect(toList());
    }

    private JurisdictionDisplayProperties[] toResponse(List<Jurisdiction> jurisdictionsDefinition) {
        return jurisdictionsDefinition.stream().map(jurisdictionMapper::toResponse)
        .toArray(JurisdictionDisplayProperties[]::new);
    }
}
