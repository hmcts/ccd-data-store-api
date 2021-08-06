package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;



@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CaseDocumentMetadataMapper {

    uk.gov.hmcts.reform.ccd.document.am.model.CaseDocumentsMetadata
        convertToAmClientCaseDocumentsMetadata(CaseDocumentsMetadata caseDocumentsMetadata);
}
