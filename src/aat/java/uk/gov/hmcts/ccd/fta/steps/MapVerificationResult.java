package uk.gov.hmcts.ccd.fta.steps;

import java.util.ArrayList;
import java.util.List;

public class MapVerificationResult {

    public static final MapVerificationResult DEFAULT_VERIFIED = new MapVerificationResult(null, true,
            "Map has expected content", 0, 0);

    private String field;
    private boolean verified;
    private String summary;
    private List<String> unexpectedFields;
    private List<String> unavailableFields;
    private List<String> badValueFields;
    private List<MapVerificationResult> badSubmaps;
    private int currentDepth;
    private int maxMessageDepth;

    public MapVerificationResult(String field, boolean verified, String summary, int currentDepth,
            int maxMessageDepth) {
        this(field, verified, summary, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                currentDepth,
                maxMessageDepth);
    }

    public MapVerificationResult(String field, boolean verified, String summary, List<String> unexpectedFields,
            List<String> unavailableFields,
            List<String> badValueFields, List<MapVerificationResult> badSubmaps, int currentDepth,
            int maxMessageDepth) {
        super();
        this.field = field;
        this.verified = verified;
        this.summary = summary;
        this.unexpectedFields = unexpectedFields;
        this.unavailableFields = unavailableFields;
        this.badValueFields = badValueFields;
        this.badSubmaps = badSubmaps;
        this.currentDepth = currentDepth;
        this.maxMessageDepth = maxMessageDepth;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getUnexpectedFields() {
        return unexpectedFields;
    }

    public void setUnexpectedFields(List<String> unexpectedFields) {
        this.unexpectedFields = unexpectedFields;
    }

    public List<String> getUnavailableFields() {
        return unavailableFields;
    }

    public void setUnavailableFields(List<String> unavailableFields) {
        this.unavailableFields = unavailableFields;
    }

    public List<String> getBadValueFields() {
        return badValueFields;
    }

    public void setBadValueFields(List<String> badValueFields) {
        this.badValueFields = badValueFields;
    }

    public List<String> getAllIssues() {
        boolean shouldReportOnlySummary = currentDepth >= maxMessageDepth;
        ArrayList<String> allIssues = new ArrayList<>();
        if (!isVerified() && summary != null) {
            allIssues.add(summary);
        }
        reportUnexpectedlyAvailables(allIssues, unexpectedFields, field, shouldReportOnlySummary);
        reportUnexpectedlyUnavailables(allIssues, unavailableFields, field, shouldReportOnlySummary);
        reportBadValues(allIssues, badValueFields, field, shouldReportOnlySummary);
        reportBadSubmaps(allIssues, badSubmaps, field, shouldReportOnlySummary);
        return allIssues;
    }

    public boolean isVerified() {
        return verified;
    }

    public static final MapVerificationResult minimalVerifiedResult(String field, int currentDepth,
            int maxMessageDepth) {
        return new MapVerificationResult(field, true, field + ": Map has expected content.", currentDepth,
                maxMessageDepth);
    }

    public static final MapVerificationResult minimalUnverifiedResult(String field, int currentDepth,
            int maxMessageDepth) {
        return new MapVerificationResult(field, false, field + ": Map does not have expected content.", currentDepth,
                maxMessageDepth);
    }

    private void reportUnexpectedlyAvailables(List<String> allIssues, List<String> unexpectedFields,
            String fieldPrefix, boolean shouldReportOnlySummary) {
        for (String unexpectedField : unexpectedFields) {
            if (!shouldReportOnlySummary) {
                String message = fieldPrefix + "." + unexpectedField + " is unexpected.";
                allIssues.add(message);
            }
        }
        if (unexpectedFields.size() > 0 && shouldReportOnlySummary) {
            String message = fieldPrefix + " has unexpected field(s): " + unexpectedFields;
            allIssues.add(message);
        }
    }

    private void reportUnexpectedlyUnavailables(List<String> allIssues, List<String> unavailableFields,
            String fieldPrefix, boolean shouldReportOnlySummary) {
        for (String unavailableField : unavailableFields) {
            if (!shouldReportOnlySummary) {
                String message = (fieldPrefix + "." + unavailableField)
                        + " is unavailable though it was expected to be there";
                allIssues.add(message);
            }
        }
        if (unavailableFields.size() > 0 && shouldReportOnlySummary) {
            String message = fieldPrefix + " lacks " + unavailableFields
                    + " field(s) that was/were actually expected to be there.";
            allIssues.add(message);
        }
    }

    private void reportBadValues(List<String> allIssues, List<String> badValueMessages, String fieldPrefix,
            boolean shouldReportOnlySummary) {
        for (String badValueMessage : badValueMessages) {
            if (!shouldReportOnlySummary) {
                String message = fieldPrefix + " contains a bad value: " + badValueMessage;
                allIssues.add(message);
            }
        }
        if (badValueMessages.size() > 0 && shouldReportOnlySummary) {
            String message = fieldPrefix + " contains " + badValueMessages.size() + " bad value(s): "
                    + badValueMessages;
            allIssues.add(message);
        }
    }

    private void reportBadSubmaps(List<String> allIssues, List<MapVerificationResult> badSubmapResults,
            String fieldPrefix, boolean shouldReportOnlySummary) {
        for (MapVerificationResult badResult : badSubmapResults) {
            allIssues.addAll(badResult.getAllIssues());
            // reportUnexpectedlyAvailables(allIssues, badResult.getUnexpectedFields(),
            // fieldPrefix,
            // shouldReportOnlySummary);
            // reportUnexpectedlyUnavailables(allIssues, badResult.getUnavailableFields(),
            // fieldPrefix,
            // shouldReportOnlySummary);
            // reportBadValues(allIssues, badResult.getBadValueFields(), fieldPrefix,
            // shouldReportOnlySummary);

        }
    }
}
