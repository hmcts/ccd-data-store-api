package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isEmpty;

@Slf4j
@Service
public class CaseDataIssueLogger {

    private final ApplicationParams applicationParams;

    private static final String EMPTY_COLLECTION_VALUE = "{}";

    public CaseDataIssueLogger(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    public List<String> logAnyDataIssuesIn(CaseDetails currentCaseDetails, CaseDetails newCaseDetails) {
        try {
            String jurisdiction = findJurisdiction(currentCaseDetails, newCaseDetails);
            if (shouldLogForJurisdiction(jurisdiction)) {
                return logAnyUnexpectedEmptyCollectionIssueIn(currentCaseDetails, newCaseDetails);
            }
        } catch (Exception ex) {
            log.error("Unexpected error while logging data issues in case details - ", ex);
        }

        return new ArrayList<>();
    }

    private String findJurisdiction(CaseDetails currentCaseDetails, CaseDetails newCaseDetails) {
        return (currentCaseDetails != null) ? currentCaseDetails.getJurisdiction()
            : (newCaseDetails != null) ? newCaseDetails.getJurisdiction()
            : null;
    }

    private Boolean shouldLogForJurisdiction(String jurisdiction) {
        return !isEmpty(jurisdiction)
            && this.applicationParams.getCaseDataIssueLoggingJurisdictions().contains(jurisdiction);
    }

    private List<String> logAnyUnexpectedEmptyCollectionIssueIn(CaseDetails currentCaseDetails,
                                                                 CaseDetails newCaseDetails) {
        List<String> emptyCollectionsInCaseData = findNewlyAddedEmptyCollectionIn(
            findAnyUnexpectedEmptyCollectionIssueIn(currentCaseDetails),
            findAnyUnexpectedEmptyCollectionIssueIn(newCaseDetails));

        if (!emptyCollectionsInCaseData.isEmpty()) {
            log.debug("Case reference '{}' with state '{}' contains unexpected empty value in collection(s) '{}' "
                    + "and the stacktrace - {}", newCaseDetails.getReference(), newCaseDetails.getState(),
                emptyCollectionsInCaseData, new Exception().getStackTrace());
        }

        return emptyCollectionsInCaseData;
    }

    private List<String> findNewlyAddedEmptyCollectionIn(List<String> emptyCollectionsInCurrentCaseData,
                                                                List<String> emptyCollectionsInNewCaseData) {
        return emptyCollectionsInNewCaseData.stream()
            .filter(path -> !emptyCollectionsInCurrentCaseData.contains(path))
            .collect(Collectors.toList());
    }

    private List<String> findAnyUnexpectedEmptyCollectionIssueIn(CaseDetails caseDetails) {
        List<String> emptyCollectionsInCaseData = new ArrayList<>();

        if (caseDetails != null) {
            Map<String, JsonNode> caseData = caseDetails.getData();
            if (!MapUtils.isEmpty(caseData)) {
                caseData.forEach((key, value) -> findEmptyCollectionsIn(value, key, emptyCollectionsInCaseData));
            }
        }

        return emptyCollectionsInCaseData;
    }

    private void findEmptyCollectionsIn(JsonNode node, String currentPath, List<String> emptyCollectionsInData) {
        if (node == null) {
            return;
        }

        if (node.isEmpty()) {
            if (node.toString().trim().equals(EMPTY_COLLECTION_VALUE)) {
                emptyCollectionsInData.add(currentPath);
            }
        } else {
            node.forEach(subNode -> findEmptyCollectionsIn(subNode, currentPath, emptyCollectionsInData));
        }
    }

}
