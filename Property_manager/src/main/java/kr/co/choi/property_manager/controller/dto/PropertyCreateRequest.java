package kr.co.choi.property_manager.controller.dto;

import kr.co.choi.property_manager.domain.DealType;
import kr.co.choi.property_manager.domain.Property;
import kr.co.choi.property_manager.domain.PropertyStatus;

public class PropertyCreateRequest {

    /**
     * 기존 Property 엔티티의 값들로 폼 DTO를 채운다(수정 폼 진입 시).
     * 금액은 DB에 "원" 단위로 저장돼 있으므로, 화면 표시용 "만원" 단위로 변환한다.
     */
    public static PropertyCreateRequest from(Property p) {
        PropertyCreateRequest form = new PropertyCreateRequest();
        form.setTitle(p.getTitle());
        form.setRegion(p.getRegion());
        form.setBuildingName(p.getBuildingName());
        form.setAddress(p.getAddress());
        form.setLotAddress(p.getLotAddress());
        form.setUnitNumber(p.getUnitNumber());
        form.setBuiltYear(p.getBuiltYear());
        form.setArea(p.getArea());

        form.setDealType(p.getDealType());
        form.setDeposit(p.getDepositMan());           // 만원 단위로
        form.setMonthlyRent(p.getMonthlyRentMan());
        form.setManagementFee(p.getManagementFeeMan());
        form.setStatus(p.getStatus());
        form.setExpiry(p.getExpiry());

        form.setHasElevator(p.getHasElevator());
        form.setHasParking(p.getHasParking());
        form.setRoomCount(p.getRoomCount());
        form.setPetAllowed(p.getPetAllowed());
        form.setLhAvailable(p.getLhAvailable());

        form.setEntrancePassword(p.getEntrancePassword());
        form.setHousePassword(p.getHousePassword());
        form.setTenantPhone(p.getTenantPhone());
        form.setOwnerPhone(p.getOwnerPhone());

        return form;
    }

    public String title;
    public String region;
    public String buildingName;
    public String address;
    public String lotAddress;
    public String unitNumber;

    public Integer builtYear;
    public Double area;

    public DealType dealType;
    public Long deposit;
    public Long monthlyRent;
    public Long managementFee;

    public PropertyStatus status;

    public Boolean hasElevator;
    public Boolean hasParking;
    public Integer roomCount;
    public Boolean petAllowed;
    public Boolean lhAvailable;

    public String entrancePassword;
    public String housePassword;

    public String tenantPhone;
    public String ownerPhone;

    private String expiry;

    // =========== Getter / Setter ========


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLotAddress() {
        return lotAddress;
    }

    public void setLotAddress(String lotAddress) {
        this.lotAddress = lotAddress;
    }

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public Integer getBuiltYear() {
        return builtYear;
    }

    public void setBuiltYear(Integer builtYear) {
        this.builtYear = builtYear;
    }

    public Double getArea() {
        return area;
    }

    public void setArea(Double area) {
        this.area = area;
    }

    public Long getDeposit() {
        return deposit;
    }

    public void setDeposit(Long deposit) {
        this.deposit = deposit;
    }

    public DealType getDealType() {
        return dealType;
    }

    public void setDealType(DealType dealType) {
        this.dealType = dealType;
    }

    public Long getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(Long monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public Long getManagementFee() {
        return managementFee;
    }

    public void setManagementFee(Long managementFee) {
        this.managementFee = managementFee;
    }

    public PropertyStatus getStatus() {
        return status;
    }

    public void setStatus(PropertyStatus status) {
        this.status = status;
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

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
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

    public String getEntrancePassword() {
        return entrancePassword;
    }

    public void setEntrancePassword(String entrancePassword) {
        this.entrancePassword = entrancePassword;
    }

    public String getHousePassword() {
        return housePassword;
    }

    public void setHousePassword(String housePassword) {
        this.housePassword = housePassword;
    }

    public String getTenantPhone() {
        return tenantPhone;
    }

    public void setTenantPhone(String tenantPhone) {
        this.tenantPhone = tenantPhone;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }


    public String getExpiry() {
        return expiry;
    }

    public void setExpiry(String expiry) {
        this.expiry = expiry;
    }
}
