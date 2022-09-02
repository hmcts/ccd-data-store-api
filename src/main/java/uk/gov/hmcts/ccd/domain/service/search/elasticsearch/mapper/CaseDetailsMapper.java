package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface CaseDetailsMapper {

    String EMPTY_MAP = "java(new HashMap<>())";

    @Mapping(source = "data", target = "data", defaultExpression = EMPTY_MAP)
    @Mapping(source = "dataClassification", target = "dataClassification", defaultExpression = EMPTY_MAP)
    CaseDetails dtoToCaseDetails(ElasticSearchCaseDetailsDTO caseDetailsDTO);

    List<CaseDetails> dtosToCaseDetailsList(List<ElasticSearchCaseDetailsDTO> caseDetailsDTOs);
}
