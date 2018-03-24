package uk.gov.hmcts.ccd.data.casedetails;

import java.io.Serializable;

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

}
