package kr.co.choi.property_manager.domain;

public enum DealType {
    MONTHLY("월세"),
    JEONSE("전세");

    private final String label;

    DealType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
