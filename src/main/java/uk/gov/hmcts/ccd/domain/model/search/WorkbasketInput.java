package uk.gov.hmcts.ccd.domain.model.search;

public class WorkbasketInput {

    private String label;
    private int order;
    private Field field;
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
    public int getOrder() {
        return order;
    }
    public void setOrder(int order) {
        this.order = order;
    }
    public Field getField() {
        return field;
    }
    public void setField(Field field) {
        this.field = field;
    }

}
