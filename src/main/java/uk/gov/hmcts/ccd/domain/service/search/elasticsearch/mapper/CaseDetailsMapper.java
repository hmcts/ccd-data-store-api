package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ccd.domain.model.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CaseDetailsMapper {

    List<CaseDetails> dtosToCaseDetailsList(List<ElasticSearchCaseDetailsDTO> caseDetailsDTOs);
}
