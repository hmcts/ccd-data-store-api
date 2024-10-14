package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.ToString;

@ToString
@Schema
public class SearchInputField extends CriteriaField implements Serializable {
}
