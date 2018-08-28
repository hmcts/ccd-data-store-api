package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CaseDetailsMapper {

    CaseDetails dtoToCaseDetails(ElasticSearchCaseDetailsDTO caseDetailsDTO);

    List<CaseDetails> dtosToCaseDetailsList(List<ElasticSearchCaseDetailsDTO> caseDetailsDTOs);
}