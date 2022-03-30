package uk.gov.hmcts.ccd.domain.service.common;

public class PathFinderCompanion {
    //    public List<String> extractFieldTypePaths(final Map<String, JsonNode> data,
    //                                              final List<CaseFieldDefinition> caseFieldDefinitions,
    //                                              final String fieldIdPrefix,
    //                                              final List<String> paths,
    //                                              final String fieldType) {
    //        return (data == null)
    //            ? Collections.emptyList()
    //            : data.entrySet().stream()
    //            .map(caseDataPair -> caseFieldDefinitions.stream()
    //                .filter(caseField -> caseField.getId().equalsIgnoreCase(caseDataPair.getKey()))
    //                .findAny() // TODO: Is this correct ???
    //                .map(caseField -> extractField(
    //                    caseDataPair.getKey(),
    //                    caseDataPair.getValue(),
    //                    caseField,
    //                    fieldIdPrefix,
    //                    paths,
    //                    fieldType
    //                ))
    //                .orElseGet(Collections::emptyList))
    //            .flatMap(List::stream)
    //            .collect(Collectors.toList());
    //    }
}
