package uk.gov.hmcts.ccd.data.user;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
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
    public UserService(@Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository,
                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) CaseDefinitionRepository caseDefinitionRepository,
                       JurisdictionMapper jurisdictionMapper) {
        this.userRepository = userRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.jurisdictionMapper = jurisdictionMapper;
    }

    public CompletableFuture<UserProfile> getUserProfileAsync() {
        long start = System.nanoTime();
        final UserProfile userProfile = new UserProfile();
        CompletableFuture<UserDefault> userDefaultFuture = userRepository.getUserDetailsAsync()
                .whenComplete((p,t) -> {
                    LOG.debug("retrieved user details. duration: {}", (System.nanoTime() - start)/1_000_000);
                })
                .thenCompose(idamProperties -> {
                    String userId = idamProperties.getEmail();
                    userProfile.getUser().setIdamProperties(idamProperties);
                    long start2 = System.nanoTime();
                    CompletableFuture<UserDefault> userDefaultSettingsAsync = userRepository.getUserDefaultSettingsAsync(userId);
                    userDefaultSettingsAsync.whenComplete((userDefaultSettings, t) -> {
                        LOG.debug("retrieved user default settings. duration: {}", (System.nanoTime() - start2)/1_000_000);
                    });
                    return userDefaultSettingsAsync;
                });

        long start3 = System.nanoTime();
        CompletableFuture<List<Jurisdiction>> jurisdictionDefsFuture = caseDefinitionRepository.getAllJurisdictionsAsync();

        jurisdictionDefsFuture.whenComplete((j,t) -> {
            LOG.debug("retrieved jurisdictions. duration: {}", (System.nanoTime() - start3)/1_000_000);
        });

        return userDefaultFuture.thenCombine(jurisdictionDefsFuture, ((userDefault, jurisdictions) -> {
            List<String> userJurisdictions = userDefault.getJurisdictionsId();

            JurisdictionDisplayProperties[] resultJurisdictions = toResponse(userJurisdictions, jurisdictions);

            userProfile.setJurisdictions(resultJurisdictions);

            final WorkbasketDefault workbasketDefault = new WorkbasketDefault();
            workbasketDefault.setJurisdictionId(userDefault.getWorkBasketDefaultJurisdiction());
            workbasketDefault.setCaseTypeId(userDefault.getWorkBasketDefaultCaseType());
            workbasketDefault.setStateId(userDefault.getWorkBasketDefaultState());
            userProfile.getDefaultSettings().setWorkbasketDefault(workbasketDefault);
            LOG.debug("returning user profile. duration: {}", (System.nanoTime() - start)/1_000_000);

            return userProfile;
        }));
    }

    private JurisdictionDisplayProperties[] toResponse(List<String> userJurisdictions, List<Jurisdiction>
            jurisdictions) {
        return userJurisdictions.stream().map(id -> {
            Optional<Jurisdiction> definition = jurisdictions.stream().filter(def -> def.getId().equals(id)).findAny();
            if (!definition.isPresent()) {
                LOG.warn("Could not retrieve definition for jurisdiction '{}'", id);
            }
            return definition.map(jurisdictionMapper::toResponse);
        }).filter(Optional::isPresent).map(Optional::get).toArray(JurisdictionDisplayProperties[]::new);
    }

}
