package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;

import java.util.List;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CaseDetailsMapper {

    List<CaseDetails> dtosToCaseDetailsList(List<ElasticSearchCaseDetailsDTO> caseDetailsDTOs);
}