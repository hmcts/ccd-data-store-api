package uk.gov.hmcts.ccd.data.caseaccess;

import java.util.List;
import org.apache.commons.collections.ListUtils;

public class CaseAssignedUserRolesKey {

    private final List<Long> caseIds;

    private final List<String> userIds;

    public CaseAssignedUserRolesKey(List<Long> caseIds, List<String> userIds) {
        this.caseIds = caseIds;
        this.userIds = userIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CaseAssignedUserRolesKey that = (CaseAssignedUserRolesKey) o;
        if (caseIds == null || that.caseIds == null || caseIds.size() != that.caseIds.size()) {
            return false;
        }

        if (userIds == null || that.userIds == null || userIds.size() != that.userIds.size()) {
            return false;
        }
        return caseIds.containsAll(that.caseIds) && userIds.containsAll(that.userIds);
    }

    @Override
    public int hashCode() {
        int caseIdsHashCode = ListUtils.hashCodeForList(caseIds);
        int result = (caseIdsHashCode ^ (caseIdsHashCode >>> 32));
        return 31 * result +  ListUtils.hashCodeForList(userIds);
    }
}
