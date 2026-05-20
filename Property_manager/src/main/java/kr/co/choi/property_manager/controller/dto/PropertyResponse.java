package kr.co.choi.property_manager.controller.dto;

import kr.co.choi.property_manager.domain.DealType;
import kr.co.choi.property_manager.domain.Property;
import kr.co.choi.property_manager.domain.PropertyPhoto;
import kr.co.choi.property_manager.domain.PropertyStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 매물 단건/목록 응답 DTO.
 *
 * <p>엔티티({@link Property})를 직접 노출하지 않는 이유:
 * <ul>
 *   <li>민감 필드(현관/집 비밀번호 등)를 실수로 노출하는 사고 방지</li>
 *   <li>지연 로딩 컬렉션이 직렬화 시점에 펼쳐져 N+1을 유발하는 사고 방지</li>
 *   <li>화면 표시용 라벨(만원 단위, getDealTypeLabel) 같은 변환을 한곳에서 정리</li>
 * </ul>
 *
 * <p>금액은 화면 친화적인 "만원" 단위로 내려준다.
 */
public record PropertyResponse(
        Long id,
        String title,
        String region,
        String buildingName,
        String address,
        String lotAddress,
        String unitNumber,
        Integer builtYear,
        Double area,
        Double lat,
        Double lng,
        DealType dealType,
        String dealTypeLabel,
        Long depositMan,
        Long monthlyRentMan,
        Long managementFeeMan,
        PropertyStatus status,
        String statusLabel,
        Boolean hasElevator,
        Boolean hasParking,
        Integer roomCount,
        Boolean petAllowed,
        Boolean lhAvailable,
        String expiry,
        String tenantPhone,
        String ownerPhone,
        List<PhotoResponse> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PropertyResponse from(Property p) {
        return new PropertyResponse(
                p.getId(),
                p.getTitle(),
                p.getRegion(),
                p.getBuildingName(),
                p.getAddress(),
                p.getLotAddress(),
                p.getUnitNumber(),
                p.getBuiltYear(),
                p.getArea(),
                p.getLat(),
                p.getLng(),
                p.getDealType(),
                p.getDealTypeLabel(),
                p.getDepositMan(),
                p.getMonthlyRentMan(),
                p.getManagementFeeMan(),
                p.getStatus(),
                p.getStatusLabel(),
                p.getHasElevator(),
                p.getHasParking(),
                p.getRoomCount(),
                p.getPetAllowed(),
                p.getLhAvailable(),
                p.getExpiry(),
                p.getTenantPhone(),
                p.getOwnerPhone(),
                p.getPhotos().stream().map(PhotoResponse::from).toList(),
                null,   // createdAt — Property 엔티티에 getter 없음. 필요 시 추가
                null    // updatedAt — 동일
        );
    }

    /**
     * 사진 정보를 간소화한 응답 DTO.
     */
    public record PhotoResponse(Long id, String url, String originalName) {
        public static PhotoResponse from(PropertyPhoto p) {
            return new PhotoResponse(p.getId(), p.getUrl(), p.getOriginalName());
        }
    }
}
