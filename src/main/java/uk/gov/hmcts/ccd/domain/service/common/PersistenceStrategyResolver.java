package uk.gov.hmcts.ccd.domain.service.common;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

/**
 * Resolves the persistence strategy for a given case.
 *
 * This service determines whether a case's mutable data should be handled by the
 * internal CCD database or delegated to an external, case-type-specific service.
 */
@Service
@Slf4j
@ConfigurationProperties("ccd.decentralised")
public class PersistenceStrategyResolver {

    private final CaseDetailsRepository caseDetailsRepository;

    /**
     * A map where the key is lowercase(case-type-id) and the value is the base URL
     * of the owning service responsible for its persistence.
     * e.g., 'MyCaseType': 'http://my-service-host:port'
     *
     * This can be set via application properties and environment variables.
     */
    @NotNull
    private Map<String, URI> caseTypeServiceUrls;


    // TODO: Using the DefaultCaseDetailsRepository and looking up the whole case is inefficient.
    // We should lookup only the case type and cache it per request.
    @Autowired
    public PersistenceStrategyResolver(@Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                           CaseDetailsRepository caseDetailsRepository ) {
        this.caseDetailsRepository = caseDetailsRepository;
    }

    /**
     * Sets the case type service URLs when the application starts.
     * The keys in the map are converted to lowercase to ensure case-insensitivity.
     */
    public void setCaseTypeServiceUrls(Map<String, URI> caseTypeServiceUrls) {
        this.caseTypeServiceUrls = new HashMap<>();
        caseTypeServiceUrls.forEach((key, value) ->
            this.caseTypeServiceUrls.put(key.toLowerCase(), value)
        );
    }

    public boolean isDecentralised(CaseDetails details) {
        return getCaseTypeServiceUrl(details.getCaseTypeId()).isPresent();
    }

    public boolean isDecentralised(long reference) {
        return isDecentralised(String.valueOf(reference));
    }

    public boolean isDecentralised(String reference) {
        var details = caseDetailsRepository.findByReferenceWithNoAccessControl(reference).get();
        return getCaseTypeServiceUrl(details.getCaseTypeId()).isPresent();
    }

    public URI resolveUriOrThrow(String caseReference) {
        Optional<CaseDetails> caseDetailsOptional = caseDetailsRepository.findByReference(caseReference);

        if (caseDetailsOptional.isEmpty()) {
            throw new UnsupportedOperationException();
        }

        return getCaseTypeServiceUrl(caseDetailsOptional.get().getCaseTypeId()).orElseThrow();
    }


    public URI resolveUriOrThrow(CaseDetails caseDetails) {
        Optional<URI> url = getCaseTypeServiceUrl(caseDetails.getCaseTypeId());
        if (url.isPresent()) {
            return url.get();
        }
        String message = String.format(
            "Operation failed: No decentralised persistence route configured for case type %s",
            caseDetails.getCaseTypeId()
        );
        throw new UnsupportedOperationException(message);
    }


    private Optional<URI> getCaseTypeServiceUrl(String caseTypeId) {
        return Optional.ofNullable(caseTypeServiceUrls.get(caseTypeId.toLowerCase()));
    }
}
