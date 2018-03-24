package uk.gov.hmcts.ccd.domain.model.search;

public class SearchInput {

    private String label;
    private int order;
    private Field field;
    public Field getField() {
        return field;
    }
    public void setField(Field field) {
        this.field = field;
    }
    public int getOrder() {
        return order;
    }
    public void setOrder(int order) {
        this.order = order;
    }
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }

}
