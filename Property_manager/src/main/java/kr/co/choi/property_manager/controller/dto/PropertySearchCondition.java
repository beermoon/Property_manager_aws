package kr.co.choi.property_manager.controller.dto;

import kr.co.choi.property_manager.domain.DealType;

public class PropertySearchCondition {
    private String keyword;
    private String region;
    private DealType dealType;

    private Long minDeposit;      // 만원 단위
    private Long maxDeposit;
    private Long minMonthlyRent;  // 만원 단위
    private Long maxMonthlyRent;

    private Integer minBuiltYear;
    private Integer maxBuiltYear;

    private Double minArea;
    private Double maxArea;

    private Integer roomCount;
    private Boolean hasElevator;
    private Boolean hasParking;
    private Boolean petAllowed;
    private Boolean lhAvailable;

    private String expiry; // "4/27" 같은 문자열

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public DealType getDealType() {
        return dealType;
    }

    public void setDealType(DealType dealType) {
        this.dealType = dealType;
    }

    public Long getMinDeposit() {
        return minDeposit;
    }

    public void setMinDeposit(Long minDeposit) {
        this.minDeposit = minDeposit;
    }

    public Long getMaxDeposit() {
        return maxDeposit;
    }

    public void setMaxDeposit(Long maxDeposit) {
        this.maxDeposit = maxDeposit;
    }

    public Long getMinMonthlyRent() {
        return minMonthlyRent;
    }

    public void setMinMonthlyRent(Long minMonthlyRent) {
        this.minMonthlyRent = minMonthlyRent;
    }

    public Integer getMinBuiltYear() {
        return minBuiltYear;
    }

    public void setMinBuiltYear(Integer minBuiltYear) {
        this.minBuiltYear = minBuiltYear;
    }

    public Long getMaxMonthlyRent() {
        return maxMonthlyRent;
    }

    public void setMaxMonthlyRent(Long maxMonthlyRent) {
        this.maxMonthlyRent = maxMonthlyRent;
    }

    public Integer getMaxBuiltYear() {
        return maxBuiltYear;
    }

    public void setMaxBuiltYear(Integer maxBuiltYear) {
        this.maxBuiltYear = maxBuiltYear;
    }

    public Double getMinArea() {
        return minArea;
    }

    public void setMinArea(Double minArea) {
        this.minArea = minArea;
    }

    public Double getMaxArea() {
        return maxArea;
    }

    public void setMaxArea(Double maxArea) {
        this.maxArea = maxArea;
    }

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    public Boolean getHasElevator() {
        return hasElevator;
    }

    public void setHasElevator(Boolean hasElevator) {
        this.hasElevator = hasElevator;
    }

    public Boolean getHasParking() {
        return hasParking;
    }

    public void setHasParking(Boolean hasParking) {
        this.hasParking = hasParking;
    }

    public Boolean getPetAllowed() {
        return petAllowed;
    }

    public void setPetAllowed(Boolean petAllowed) {
        this.petAllowed = petAllowed;
    }

    public Boolean getLhAvailable() {
        return lhAvailable;
    }

    public void setLhAvailable(Boolean lhAvailable) {
        this.lhAvailable = lhAvailable;
    }

    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }
}