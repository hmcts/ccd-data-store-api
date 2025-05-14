package uk.gov.hmcts.rd;

import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.ccd.data.ReferenceDataRepository;
import uk.gov.hmcts.ccd.domain.model.refdata.BuildingLocation;
import uk.gov.hmcts.ccd.domain.model.refdata.CourtVenue;

import java.time.LocalDate;
import java.util.List;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@PactTestFor(providerName = "referenceData_location", port = "8090")
@SpringBootTest({
    "reference.data.api.url:http://localhost:8090"
})
public class BuildingLocationDetailsConsumerTest extends AbstractCcdConsumerTest {

    @Inject
    private ReferenceDataRepository referenceDataRepository;

    @Pact(provider = "referenceData_location", consumer = "ccd_dataStoreApi")
    public V4Pact buildingLocationDetailsFragment(PactBuilder builder) {
        return builder
            .usingLegacyDsl()
            .given("Building Location details exist for the request provided")
            .uponReceiving("A request for Building Location details")
            .path("/refdata/location/building-locations")
            .method("GET")
            .matchHeader("Authorization", "Bearer .*", "Bearer UserAuthToken")
            .matchHeader("ServiceAuthorization", "Bearer .*", "ServiceToken")
            .willRespondWith()
            .status(200)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(getBuildingLocationDetailsResponseBody())
            .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "buildingLocationDetailsFragment")
    void verifyBuildingLocationDetailsPact() {

        List<BuildingLocation> buildingLocations = referenceDataRepository.getBuildingLocations();
        assertNotNull(buildingLocations, "Building locations list should not be null");

        BuildingLocation location = buildingLocations.get(0);
        assertEquals("BL-ID", location.getBuildingLocationId());
        assertEquals("54 TEST ROAD", location.getBuildingLocationName());
        assertEquals("123", location.getEpimmsId());
        assertEquals("LIVE", location.getBuildingLocationStatus());
        assertEquals("North", location.getArea());
        assertEquals("Midlands", location.getRegion());
        assertEquals("1", location.getRegionId());
        assertEquals("NBC", location.getClusterName());
        assertEquals("1", location.getClusterId());
        assertEquals("https://testUrl.com", location.getCourtFinderUrl());
        assertEquals("T33ST", location.getPostcode());
        assertEquals("TEST ROAD", location.getAddress());

        CourtVenue courtVenue = location.getCourtVenues().get(0);
        assertNotNull(location.getCourtVenues());
        assertEquals(1, location.getCourtVenues().size(), "Expected 1 court venue");
        assertEquals("CV-ID-1", courtVenue.getCourtVenueId());
        assertEquals("123", courtVenue.getEpimmsId());
        assertEquals("Aberdeen Tribunal Hearing Centre 1", courtVenue.getSiteName());
        assertEquals("1", courtVenue.getRegionId());
        assertEquals("Midlands", courtVenue.getRegion());
        assertEquals("Tribunal", courtVenue.getCourtType());
        assertEquals("10", courtVenue.getCourtTypeId());
        assertEquals("1", courtVenue.getClusterId());
        assertEquals("NBC", courtVenue.getClusterName());
        assertEquals("Yes", courtVenue.getOpenForPublic());
        assertEquals("1 Tribunal Street", courtVenue.getCourtAddress());
        assertEquals("AB11 6LT", courtVenue.getPostcode());
        assertEquals("01234 567890", courtVenue.getPhoneNumber());
        assertEquals(LocalDate.parse("2021-01-01"), courtVenue.getClosedDate());
        assertEquals("12345", courtVenue.getCourtLocationCode());
        assertEquals("DX 123456", courtVenue.getDxAddress());
        assertEquals("Canolfan Wrandawiad Abertawe 1", courtVenue.getWelshSiteName());
        assertEquals("1 Stryd y Tribiwnlys", courtVenue.getWelshCourtAddress());
        assertEquals("Open", courtVenue.getCourtStatus());
        assertEquals(LocalDate.parse("2020-01-01"), courtVenue.getCourtOpenDate());
        assertEquals("Aberdeen Tribunal Hearing Centre", courtVenue.getCourtName());
    }

    protected String getBuildingLocationDetailsResponseBody() {
        return """
            [
                {
                    "building_location_id": "BL-ID",
                    "building_location_name": "54 TEST ROAD",
                    "epimms_id": "123",
                    "building_location_status": "LIVE",
                    "area": "North",
                    "region": "Midlands",
                    "region_id": "1",
                    "cluster_name": "NBC",
                    "cluster_id": "1",
                    "court_finder_url": "https://testUrl.com",
                    "postcode": "T33ST",
                    "address": "TEST ROAD",
                    "court_venues": [
                              {
                                "court_venue_id": "CV-ID-1",
                                "epimms_id": "123",
                                "site_name": "Aberdeen Tribunal Hearing Centre 1",
                                "region_id": "1",
                                "region": "Midlands",
                                "court_type": "Tribunal",
                                "court_type_id": "10",
                                "cluster_id": "1",
                                "cluster_name": "NBC",
                                "open_for_public": "Yes",
                                "court_address": "1 Tribunal Street",
                                "postcode": "AB11 6LT",
                                "phone_number": "01234 567890",
                                "closed_date": "2021-01-01",
                                "court_location_code": "12345",
                                "dx_address": "DX 123456",
                                "welsh_site_name": "Canolfan Wrandawiad Abertawe 1",
                                "welsh_court_address": "1 Stryd y Tribiwnlys",
                                "court_status": "Open",
                                "court_open_date": "2020-01-01",
                                "court_name": "Aberdeen Tribunal Hearing Centre"
                              }
                        ]
                }
            ]
            """;
    }
}
