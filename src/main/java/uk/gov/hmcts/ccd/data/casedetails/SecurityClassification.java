package uk.gov.hmcts.ccd.data.casedetails;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum SecurityClassification implements Serializable {
    PUBLIC(1), PRIVATE(2), RESTRICTED(3);

    private final int rank;

    SecurityClassification(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public Boolean higherOrEqualTo(SecurityClassification otherClassification) {
        return this.rank >= otherClassification.rank;
    }

    public List<String> getClassificationsLowerOrEqualTo() {
        return Stream.of(values()).filter(this::higherOrEqualTo).map(Enum::name).collect(Collectors.toList());
    }

}
