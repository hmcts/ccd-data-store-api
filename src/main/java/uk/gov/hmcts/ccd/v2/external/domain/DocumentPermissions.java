package uk.gov.hmcts.ccd.v2.external.domain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode
@Builder
public class DocumentPermissions {
    private String id;
    private List<Permission> permissions;
}
