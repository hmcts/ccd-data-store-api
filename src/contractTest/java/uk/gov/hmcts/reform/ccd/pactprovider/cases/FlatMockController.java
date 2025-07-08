package uk.gov.hmcts.reform.ccd.pactprovider.cases;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/internal/searchCases")
public class FlatMockController {

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchCases(@RequestParam("ctid") String caseTypeId,
                                                           @RequestBody Map<String, Object> query) {

        Map<String, Object> caseData = new HashMap<>();
        caseData.put("applicantSolicitorEmailAddress", "prl-e2etestsolicitor@mailinator.com");
        caseData.put("caseTypeOfApplication", "C100");
        caseData.put("confidentialDetails", Map.of("isConfidentialDetailsAvailable", "No"));
        caseData.put("childrenKnownToLocalAuthority", "no");
        caseData.put("children", List.of());
        caseData.put("respondents", List.of());
        caseData.put("applicants", List.of());
        caseData.put("applicantsConfidentialDetails", List.of(Map.of()));
        caseData.put("courtName", "West London Family Court");
        caseData.put("dateSubmitted", "2022-08-22");

        caseData.put("miamExemptionsTable", Map.of(
            "reasonsForMiamExemption", "Urgency",
            "domesticViolenceEvidence", "",
            "urgencyEvidence", "Any delay caused by MIAM would cause unreasonable hardship to the prospective applicant",
            "childProtectionEvidence", "",
            "previousAttendenceEvidence", "",
            "otherGroundsEvidence", ""
        ));

        caseData.put("otherPeopleInTheCaseTable", List.of(Map.of(
            "id", "a5d86587-ba06-4db0-8620-10ef472af3e5",
            "value", Map.of(
                "address", Map.of(),
                "relationshipToChild", List.of(Map.of())
            )
        )));

        caseData.put("solicitorName", "E2E Test Solicitor");

        caseData.put("submitAndPayDownloadApplicationLink", Map.of(
            "document_filename", "Draft_C100_application.pdf",
            "document_id", "e7226e49-fc92-4c12-bac5-e3e50ee4ff15"
        ));

        caseData.put("summaryTabForOrderAppliedFor", Map.of(
            "ordersApplyingFor", "Child Arrangements Order",
            "typeOfChildArrangementsOrder", "Live with order"
        ));

        Map<String, Object> caseMap = new HashMap<>();
        caseMap.put("id", 16611647);
        caseMap.put("jurisdiction", "PRIVATELAW");
        caseMap.put("state", "SUBMITTED_PAID");
        caseMap.put("caseTypeOfApplication", "C100");
        caseMap.put("case_type_id", "PRLAPPS");
        caseMap.put("created_date", "2022-08-22T10:39:43.49");
        caseMap.put("last_modified", "2022-08-22T10:44:54.055");
        caseMap.put("case_data", caseData);

        Map<String, Object> response = new HashMap<>();
        response.put("total", 1);
        response.put("cases", List.of(caseMap));

        return ResponseEntity.ok(response);
    }
}
