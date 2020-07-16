package uk.gov.hmcts.ccd.data.casedetails.search;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

@Named
@Singleton
public class CriterionFactory {

    public List<Criterion> build(MetaData metadata, Map<String, String> params) {
        List<Criterion> result = params.entrySet()
                .stream()
                .map(this::buildFromEntry)
                .collect(Collectors.toList());
        result.addAll(buildFromMetaData(metadata));
        return result;
    }

    private Criterion buildFromEntry(Entry<String, String> entry) {
        return new FieldDataCriterion(entry.getKey(), entry.getValue());
    }

    private List<Criterion> buildFromMetaData(MetaData metadata) {
        List<Criterion> result = new ArrayList<>();

        ifPresentAndNotBlank(Optional.ofNullable(metadata.getCaseTypeId()), ct ->
                result.add(new MetaDataCriterion(CaseDetailsEntity.CASE_TYPE_ID_FIELD_COL, ct)));

        ifPresentAndNotBlank(Optional.ofNullable(metadata.getJurisdiction()), j ->
                result.add(new MetaDataCriterion(CaseDetailsEntity.JURISDICTION_FIELD_COL, j)));

        ifPresentAndNotBlank(metadata.getCaseReference(), r ->
                result.add(new MetaDataCriterion(CaseDetailsEntity.REFERENCE_FIELD_COL, r)));

        ifPresentAndNotBlank(metadata.getState(), s ->
                result.add(new MetaDataCriterion(CaseDetailsEntity.STATE_FIELD_COL, s)));

        ifPresentAndNotBlank(metadata.getCreatedDate(), cd ->
                result.add(new MetaDataCriterion("date(" + CaseDetailsEntity.CREATED_DATE_FIELD_COL + ")", cd)));

        ifPresentAndNotBlank(metadata.getLastModifiedDate(), lm ->
                result.add(new MetaDataCriterion("date(" + CaseDetailsEntity.LAST_MODIFIED_FIELD_COL + ")", lm)));

        ifPresentAndNotBlank(metadata.getLastStateModifiedDate(), lsm ->
            result.add(new MetaDataCriterion("date(" + CaseDetailsEntity.LAST_STATE_MODIFIED_DATE_FIELD_COL + ")", lsm)));

        ifPresentAndNotBlank(metadata.getSecurityClassification(), sc ->
                result.add(new MetaDataCriterion(CaseDetailsEntity.SECURITY_CLASSIFICATION_FIELD_COL, sc.toUpperCase())));

        return result;
    }

    private void ifPresentAndNotBlank(Optional<String> metadata, Consumer<String> metadataConsumer) {
        metadata.ifPresent(m -> {
            if (isNotBlank(m)) {
                metadataConsumer.accept(m);
            }
        });
    }

}
