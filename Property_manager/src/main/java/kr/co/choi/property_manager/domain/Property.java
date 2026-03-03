package kr.co.choi.property_manager.domain;

import jakarta.persistence.*;
import kr.co.choi.property_manager.controller.dto.PropertyCreateRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ======== 기본 정보 ==========
    @Column(nullable = false)
    private String title;

    private String region;   // 구역
    private String buildingName; // 건물명

    @Column(nullable = true)    // 도로명 주소
    private String address;

    @Column(nullable = false)
    private String lotAddress;     // 지번
    private String unitNumber;     // 호수

    private Integer builtYear;     // 준공연도
    private Double area;            // 평수

    // ===== 지도 =====
    private Double lat;
    private Double lng;

    // ========== 거래정보 =========
    @Enumerated(EnumType.STRING)
    private DealType dealType;  // JEONSE / MONTHLY

    private Long deposit;   // 보증금
    private Long monthlyRent;   // 월세
    private Long managementFee; // 관리비

    @Enumerated(EnumType.STRING)
    private PropertyStatus status; // VACANT / CONTRACTED

    // =========== 옵션 =======
    private Boolean hasElevator;    // 엘베 유무
    private Boolean hasParking; // 주차 유무
    private Integer roomCount;  // 1~4
    private Boolean petAllowed; // 반려동물 가능 불가능
    private Boolean lhAvailable;    // LH 인지 아닌지

    // ====== 내부 관리 ==========
    private String entrancePassword;    // 공동현관 비밀번호
    private String housePassword;    // 공동현관 비밀번호

    // ====== 연락 ==========
    private String tenantPhone; // 세입자 전화번호
    private String ownerPhone; // 집주인 전화번호

    // ====== 메모 ==========
    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<Memo> memos = new ArrayList<>();


    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<PropertyPhoto> photos = new ArrayList<>();

    public void addPhoto(PropertyPhoto photo) {
        this.photos.add(photo);
        photo.setProperty(this);
    }

    // 만기
    private String expiry;

    public Property() {}

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ======= Getter ==========

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getRegion() { return region; }
    public String getBuildingName() { return buildingName; }
    public String getAddress() { return address; }
    public String getLotAddress() { return lotAddress; }
    public String getUnitNumber() { return unitNumber; }
    public Integer getBuiltYear() { return builtYear; }
    public Double getArea() { return area; }
    public Double getLat() { return lat; }
    public Double getLng() { return lng; }
    public DealType getDealType() { return dealType; }
    public Long getDeposit() { return deposit; }
    public Long getMonthlyRent() { return monthlyRent; }
    public Long getManagementFee() { return managementFee; }
    public PropertyStatus getStatus() { return status; }
    public Boolean getHasElevator() { return hasElevator; }
    public Boolean getHasParking() { return hasParking; }
    public Integer getRoomCount() { return roomCount; }
    public Boolean getPetAllowed() { return petAllowed; }
    public Boolean getLhAvailable() { return lhAvailable; }
    public String getEntrancePassword() { return entrancePassword; }
    public String getHousePassword() { return housePassword; }
    public String getTenantPhone() { return tenantPhone; }
    public String getOwnerPhone() { return ownerPhone; }
    public String getExpiry() {return expiry;}



    // =========== 위치 업데이트 ========
    public void updateLocation(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }


    public Property(String title, String address, PropertyStatus status, DealType dealType) {
        this.title = title;
        this.address = address;
        this.status = status;
        this.dealType = dealType;
    }

    // ========= 연관관계 편의 메서드 ===========
    public void addMemo(Memo memo) {
        this.memos.add(memo);
        memo.setProperty(this);
    }
    public void removeMemo(Memo memo) {
        this.memos.remove(memo);
        memo.setProperty(null);
    }

    private Long toWon(Long man) {
        return man == null ? null : man * 10_000;
    }


    public void updateAll(PropertyCreateRequest req) {

        // ===== 기본 정보 =====
        this.title = req.getTitle();
        this.region = req.getRegion();
        this.buildingName = req.getBuildingName();
        this.address = req.getAddress();
        this.lotAddress = req.getLotAddress();
        this.unitNumber = req.getUnitNumber();
        this.builtYear = req.getBuiltYear();
        this.area = req.getArea();

        // ===== 거래 정보 =====
        this.dealType = (req.getDealType() == null) ? DealType.MONTHLY : req.getDealType();
        this.deposit = toWon(req.getDeposit());
        this.monthlyRent = toWon(req.getMonthlyRent());
        this.managementFee = toWon(req.getManagementFee());

        // status는 기본 VACANT로 하기로 했으니:
        this.status = (req.getStatus() == null) ? PropertyStatus.VACANT : req.getStatus();

        // ===== 옵션 =====
        this.hasElevator = req.getHasElevator();
        this.hasParking = req.getHasParking();
        this.roomCount = req.getRoomCount();
        this.petAllowed = req.getPetAllowed();
        this.lhAvailable = req.getLhAvailable();
        this.expiry = req.getExpiry();


        // ===== 보안/연락 =====
        this.entrancePassword = req.getEntrancePassword();
        this.housePassword = req.getHousePassword();
        this.tenantPhone = req.getTenantPhone();
        this.ownerPhone = req.getOwnerPhone();
    }

    public List<Memo> getMemos() { return memos; }

    public List<PropertyPhoto> getPhotos() {
        return photos;
    }

    public void removePhoto(PropertyPhoto photo) {
        this.photos.remove(photo);
        photo.setProperty(null);
    }

    public Long getDepositMan() {
        return deposit == null ? null : deposit /10_000;
    }

    public Long getMonthlyRentMan() {
        return monthlyRent == null ? null : monthlyRent / 10_000;
    }

    public Long getManagementFeeMan() {
        return managementFee == null ? null : managementFee / 10_000;
    }

    // ===== 화면 표시용 라벨 (Thymeleaf에서 쓰기) =====

    public String getDealTypeLabel() {
        return dealType == null ? "-" : dealType.getLabel(); // DealType에 getLabel() 있어야 함
    }

    public String getStatusLabel() {
        return status == null ? "-" : status.getLabel(); // PropertyStatus에 getLabel() 있어야 함
    }

    private String yn(Boolean v) {
        if (v == null) return "-";
        return v ? "있음" : "없음";
    }

    private String yesNo(Boolean v) {
        if (v == null) return "-";
        return v ? "가능" : "불가";
    }

    public String getElevatorLabel() { return yn(hasElevator); }
    public String getParkingLabel() { return yn(hasParking); }
    public String getPetLabel() { return yesNo(petAllowed); }

    public String getLhLabel() {
        if (lhAvailable == null) return "-";
        return lhAvailable ? "LH" : "일반";
    }





}