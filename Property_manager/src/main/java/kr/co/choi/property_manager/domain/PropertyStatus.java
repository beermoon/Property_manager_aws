package kr.co.choi.property_manager.domain;

public enum PropertyStatus {
    VACANT("공실"),
    CONTRACTED("계약완료");

    private final String label;

    PropertyStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
