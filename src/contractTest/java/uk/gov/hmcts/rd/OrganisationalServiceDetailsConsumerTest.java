package uk.gov.hmcts.rd;


import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.domain.model.refdata.ServiceReferenceData;
import uk.gov.hmcts.ccd.v2.external.controller.ContractTestSecurityUtils;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@PactTestFor(providerName = "referenceData_location", port = "8091")
@SpringBootTest({
    "reference.data.api.url:http://localhost:8091"
})
public class OrganisationalServiceDetailsConsumerTest extends AbstractCcdConsumerTest {

    @Autowired
    ContractTestSecurityUtils securityUtils;

    @Inject
    private ReferenceDataRepository referenceDataRepository;

    @Pact(provider = "referenceData_location", consumer = "ccd_dataStoreApi")
    public V4Pact organisationalServiceDetailsFragment(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Organisational Service details exist")
            .uponReceiving("A request for organisational service details")
            .path("/refdata/location/orgServices")
            .method("GET")
            .matchHeader("Authorization", "Bearer .*", "Bearer UserAuthToken")
            .matchHeader("ServiceAuthorization", "Bearer .*", "ServiceToken")
            .willRespondWith()
            .status(200)
            .matchHeader(HttpHeaders.CONTENT_TYPE, "application\\/json", MediaType.APPLICATION_JSON_VALUE)
            .body(loadFile("refdata/org_details.json"))
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "organisationalServiceDetailsFragment")
    void verifyOrganisationalServiceDetailsPact() {
        List<ServiceReferenceData> services = referenceDataRepository.getServices();

        assertNotNull(services, "Services list should not be null");
        assertFalse(services.isEmpty(), "Services list should not be empty");

        ServiceReferenceData service = services.get(0);
        assertEquals(1, service.getServiceId());
        assertEquals("orgUnit", service.getOrgUnit());
        assertEquals("BusinessArea", service.getBusinessArea());
        assertEquals("OrgSubBusinessArea", service.getSubBusinessArea());
        assertEquals("Jurisdiction", service.getJurisdiction());
        assertEquals("Civil Enforcement", service.getServiceDescription());
        assertEquals("AAA1", service.getServiceCode());
        assertEquals("Civil Enforcement", service.getServiceShortDescription());
        assertEquals("CCDSERVICENAME", service.getCcdServiceName());
        assertEquals(LocalDateTime.parse("2023-10-05T12:00:00"), service.getLastUpdate());
        assertEquals("CCDCASETYPE1", service.getCcdCaseTypes().get(0));
    }
}

