package uk.gov.hmcts.ccd.fta.data;

import uk.gov.hmcts.ccd.datastore.tests.fixture.AATCaseType;

@SuppressWarnings({"MemberName", "ParameterName"})
public class CaseData {

    private String _guid_;

    private String _extends_;

    private UserData user;

    private String jurisdiction;

    private String caseType;

    private String event;

    private AATCaseType.CaseData data;

    public String get_guid_() {
        return _guid_;
    }

    public void set_guid_(String _guid_) {
        this._guid_ = _guid_;
    }

    public String get_extends_() {
        return _extends_;
    }

    public void set_extends_(String _extends_) {
        this._extends_ = _extends_;
    }

    public UserData getUser() {
        return user;
    }

    public void setUser(UserData user) {
        this.user = user;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getCaseType() {
        return caseType;
    }

    public void setCaseType(String caseType) {
        this.caseType = caseType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public AATCaseType.CaseData getData() {
        return data;
    }

    public void setData(AATCaseType.CaseData data) {
        this.data = data;
    }
}
