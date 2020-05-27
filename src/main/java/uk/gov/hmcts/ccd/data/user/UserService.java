package uk.gov.hmcts.ccd.data.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserDefault;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.aggregated.WorkbasketDefault;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private UserRepository userRepository;
    private CaseDefinitionRepository caseDefinitionRepository;
    private JurisdictionMapper jurisdictionMapper;
    private JurisdictionsResolver jurisdictionsResolver;

    @Inject
    public UserService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository,
                       JurisdictionMapper jurisdictionMapper,
                       @Qualifier(IdamJurisdictionsResolver.QUALIFIER) JurisdictionsResolver jurisdictionsResolver) {
        this.userRepository = userRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.jurisdictionMapper = jurisdictionMapper;
        this.jurisdictionsResolver = jurisdictionsResolver;
    }

    public UserProfile getUserProfile() {

        IdamProperties idamProperties = userRepository.getUserDetails();
        String userId = idamProperties.getEmail();
        List<String> jurisdictionIds = jurisdictionsResolver.getJurisdictions();

        List<JurisdictionDefinition> jurisdictionDefinitions = new ArrayList<>();

        LOGGER.debug("Will get jurisdiction(s) '{}' from repository.", jurisdictionIds);
        jurisdictionIds.stream().forEach(id -> {
            JurisdictionDefinition jurisdictionDefinition = caseDefinitionRepository.getJurisdiction(id);
            if (jurisdictionDefinition != null) {
                jurisdictionDefinitions.add(jurisdictionDefinition);
            }
        });

        return createUserProfile(idamProperties, userId, jurisdictionDefinitions);
    }

    public List<CaseTypeDefinition> getUserCaseTypes() {
        return Arrays.stream(getUserProfile().getJurisdictions())
            .map(JurisdictionDisplayProperties::getCaseTypeDefinitions)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private UserProfile createUserProfile(IdamProperties idamProperties, String userId, List<JurisdictionDefinition> jurisdictionsDefinition) {

        JurisdictionDisplayProperties[] resultJurisdictions = toResponse(jurisdictionsDefinition);

        UserProfile userProfile = new UserProfile();
        userProfile.setJurisdictions(resultJurisdictions);
        userProfile.getUser().setIdamProperties(idamProperties);
        try {
            UserDefault userDefault = userRepository.getUserDefaultSettings(userId);
            WorkbasketDefault workbasketDefault = new WorkbasketDefault();
            workbasketDefault.setJurisdictionId(userDefault.getWorkBasketDefaultJurisdiction());
            workbasketDefault.setCaseTypeId(userDefault.getWorkBasketDefaultCaseType());
            workbasketDefault.setStateId(userDefault.getWorkBasketDefaultState());
            userProfile.getDefaultSettings().setWorkbasketDefault(workbasketDefault);
        } catch (ResourceNotFoundException ae) {
            LOGGER.debug("User Profile not exists for userId {}", userId, ae);
        }

        return userProfile;
    }

    private JurisdictionDisplayProperties[] toResponse(List<JurisdictionDefinition> jurisdictionsDefinition) {
        return jurisdictionsDefinition.stream().map(jurisdictionMapper::toResponse)
        .toArray(JurisdictionDisplayProperties[]::new);
    }
}
