package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Data
@AllArgsConstructor
public class CaseSearchResultView {

    public static final CaseSearchResultView EMPTY = new CaseSearchResultView(emptyList(), emptyList(), 0L);

    @NonNull
    private List<SearchResultViewHeaderGroup> headers;
    @NonNull
    private List<SearchResultViewItem> cases;
    @NonNull
    private Long total;

    public Optional<SearchResultViewHeaderGroup> findHeaderByCaseType(String caseTypeId) {
        return headers.stream().filter(header -> header.getMetadata().getCaseTypeId().equals(caseTypeId)).findFirst();
    }

    public Optional<SearchResultViewItem> findCaseByReference(String reference) {
        return cases.stream().filter(item -> item.getCaseId().equals(reference)).findFirst();
    }

    public List<SearchResultViewItem> findCasesByCaseType(String caseTypeId) {
        List<String> references = findHeaderByCaseType(caseTypeId)
            .map(SearchResultViewHeaderGroup::getCases)
            .orElse(emptyList());

        return cases.stream().filter(item -> references.contains(item.getCaseId())).collect(Collectors.toList());
    }
}
