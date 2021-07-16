package uk.gov.hmcts.ccd.v2.external.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@EqualsAndHashCode
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class DocumentPermissions {
    private String id;
    private List<Permission> permissions;
}
