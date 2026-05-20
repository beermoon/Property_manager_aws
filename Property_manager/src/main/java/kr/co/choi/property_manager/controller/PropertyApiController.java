package kr.co.choi.property_manager.controller;

import kr.co.choi.property_manager.controller.dto.MemoResponse;
import kr.co.choi.property_manager.controller.dto.PropertyCreateRequest;
import kr.co.choi.property_manager.controller.dto.PropertyResponse;
import kr.co.choi.property_manager.domain.DealType;
import kr.co.choi.property_manager.domain.Property;
import kr.co.choi.property_manager.domain.PropertyStatus;
import kr.co.choi.property_manager.infra.NaverGeocodingClient;
import kr.co.choi.property_manager.repository.specs.PropertySpecs;
import kr.co.choi.property_manager.service.PropertyService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 매물·메모·사진 도메인의 JSON API.
 *
 * <p>RESTful URL 디자인:
 * <ul>
 *   <li>{@code GET    /api/properties}                    매물 목록</li>
 *   <li>{@code GET    /api/properties/{id}}               매물 단건</li>
 *   <li>{@code POST   /api/properties}                    매물 생성 (JSON)</li>
 *   <li>{@code PUT    /api/properties/{id}}               매물 수정 (JSON)</li>
 *   <li>{@code DELETE /api/properties/{id}}               매물 삭제</li>
 *   <li>{@code GET    /api/properties/markers}            지도용 마커 슬림 응답</li>
 *   <li>{@code POST   /api/properties/{id}/photos}        사진 추가 (multipart)</li>
 *   <li>{@code DELETE /api/properties/{id}/photos/{pid}}  사진 삭제</li>
 *   <li>{@code GET    /api/properties/{id}/memos}         메모 목록</li>
 *   <li>{@code POST   /api/properties/{id}/memos}         메모 추가</li>
 *   <li>{@code DELETE /api/properties/{id}/memos/{mid}}   메모 삭제</li>
 *   <li>{@code GET    /api/validate-address}              지오코딩 검증</li>
 * </ul>
 *
 * <p>예외는 {@link ApiExceptionHandler}가 일관된 JSON 응답으로 매핑한다.
 */
@RestController
@RequestMapping("/api")
public class PropertyApiController {

    private final PropertyService propertyService;
    private final NaverGeocodingClient naverGeocodingClient;

    public PropertyApiController(PropertyService propertyService,
                                 NaverGeocodingClient naverGeocodingClient) {
        this.propertyService = propertyService;
        this.naverGeocodingClient = naverGeocodingClient;
    }

    // ============================================================
    //   매물 CRUD
    // ============================================================

    /**
     * 매물 목록 (필터링 지원).
     * 동일한 spec 필터를 받지만, 응답은 풀 DTO인 {@link PropertyResponse}.
     */
    @GetMapping("/properties")
    public List<PropertyResponse> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) DealType dealType,
            @RequestParam(required = false) PropertyStatus status,
            @RequestParam(required = false) Long minDeposit,
            @RequestParam(required = false) Long maxDeposit,
            @RequestParam(required = false) Long minMonthlyRent,
            @RequestParam(required = false) Long maxMonthlyRent,
            @RequestParam(required = false) Integer minBuiltYear,
            @RequestParam(required = false) Integer maxBuiltYear,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea,
            @RequestParam(required = false) Integer roomCount,
            @RequestParam(required = false) Boolean hasElevator,
            @RequestParam(required = false) Boolean hasParking,
            @RequestParam(required = false) Boolean petAllowed,
            @RequestParam(required = false) Boolean lhAvailable,
            @RequestParam(required = false) String expiry
    ) {
        Specification<Property> spec = Specification.where(PropertySpecs.keywordContains(keyword))
                .and(PropertySpecs.regionEq(region))
                .and(PropertySpecs.dealTypeEq(dealType))
                .and(PropertySpecs.statusEq(status))
                .and(PropertySpecs.depositBetween(minDeposit, maxDeposit))
                .and(PropertySpecs.monthlyRentBetween(minMonthlyRent, maxMonthlyRent))
                .and(PropertySpecs.builtYearBetween(minBuiltYear, maxBuiltYear))
                .and(PropertySpecs.areaBetween(minArea, maxArea))
                .and(PropertySpecs.roomCountEq(roomCount))
                .and(PropertySpecs.hasElevator(hasElevator))
                .and(PropertySpecs.hasParking(hasParking))
                .and(PropertySpecs.petAllowed(petAllowed))
                .and(PropertySpecs.lhAvailable(lhAvailable))
                .and(PropertySpecs.expiryContains(expiry));

        return propertyService.findAll(spec).stream()
                .map(PropertyResponse::from)
                .toList();
    }

    /**
     * 매물 단건 조회.
     * 없으면 IllegalArgumentException → {@link ApiExceptionHandler}가 404로 변환.
     */
    @GetMapping("/properties/{id}")
    public PropertyResponse get(@PathVariable Long id) {
        return PropertyResponse.from(propertyService.findById(id));
    }

    /**
     * 매물 생성. 사진은 별도 엔드포인트({@code POST /properties/{id}/photos})로 추가.
     *
     * <p>응답: {@code 201 Created} + {@code Location} 헤더 + 본문(생성된 매물).
     * 표준 REST 관례를 따른다.
     */
    @PostMapping("/properties")
    public ResponseEntity<PropertyResponse> create(@RequestBody PropertyCreateRequest request) {
        Long id = propertyService.create(request, null);
        Property created = propertyService.findById(id);
        return ResponseEntity
                .created(URI.create("/api/properties/" + id))
                .body(PropertyResponse.from(created));
    }

    /**
     * 매물 수정 (전체 교체 의미의 PUT).
     * <p>응답: {@code 200 OK} + 수정 후 본문.
     */
    @PutMapping("/properties/{id}")
    public PropertyResponse update(@PathVariable Long id,
                                   @RequestBody PropertyCreateRequest request) {
        propertyService.update(id, request, null);
        return PropertyResponse.from(propertyService.findById(id));
    }

    /**
     * 매물 삭제.
     * <p>응답: {@code 204 No Content} — 성공했지만 돌려줄 본문이 없음을 표준적으로 의미.
     */
    @DeleteMapping("/properties/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        propertyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    //   사진 (매물의 서브 자원)
    // ============================================================

    /**
     * 매물에 사진 N장을 추가. JSON이 아니라 multipart로 받는다.
     * @return 추가된 후의 매물 전체 (사진 목록 포함)
     */
    @PostMapping("/properties/{id}/photos")
    public PropertyResponse addPhotos(@PathVariable Long id,
                                      @RequestParam("photos") List<MultipartFile> photos) {
        // update를 사진만 추가하는 용도로 재사용. req에 변경할 필드 없이 빈 객체 전달은 안 됨 →
        // 별도 service 메서드를 만드는 게 더 깔끔하지만, 지금은 기존 API를 활용하기 위해
        // 현재 매물을 한 번 읽고 그 값을 그대로 전달하여 "변경 없음 + 사진만 추가"를 흉내낸다.
        Property current = propertyService.findById(id);
        PropertyCreateRequest unchanged = PropertyCreateRequest.from(current);
        propertyService.update(id, unchanged, photos);
        return PropertyResponse.from(propertyService.findById(id));
    }

    @DeleteMapping("/properties/{propertyId}/photos/{photoId}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long propertyId,
                                            @PathVariable Long photoId) {
        propertyService.deletePhoto(propertyId, photoId);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    //   메모 (매물의 서브 자원)
    // ============================================================

    @GetMapping("/properties/{id}/memos")
    public List<MemoResponse> memos(@PathVariable Long id) {
        return propertyService.findMemosByProperty(id).stream()
                .map(MemoResponse::from)
                .toList();
    }

    /**
     * 메모 추가.
     * <p>요청 바디 예시: {@code {"content": "현관 비밀번호 1234"}}
     */
    @PostMapping("/properties/{id}/memos")
    public ResponseEntity<Void> addMemo(@PathVariable Long id,
                                        @RequestBody MemoCreateRequest req) {
        propertyService.addMemo(id, req.content());
        return ResponseEntity.created(URI.create("/api/properties/" + id + "/memos")).build();
    }

    @DeleteMapping("/properties/{propertyId}/memos/{memoId}")
    public ResponseEntity<Void> deleteMemo(@PathVariable Long propertyId,
                                            @PathVariable Long memoId) {
        propertyService.deleteMemo(propertyId, memoId);
        return ResponseEntity.noContent().build();
    }

    /** 메모 생성 요청 바디. */
    public record MemoCreateRequest(String content) {}

    // ============================================================
    //   지도 마커 / 주소 검증 (기존 기능)
    // ============================================================

    /**
     * 지도용 슬림 마커 응답. 풀 매물 정보가 필요 없는 화면을 위함.
     * <p>이전 버전에서는 {@code GET /api/properties}였으나, REST 표준 CRUD와
     * 충돌하지 않도록 {@code /api/properties/markers}로 이동했다.
     */
    @GetMapping("/properties/markers")
    public List<PropertyMarkerDto> markers(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) DealType dealType,
            @RequestParam(required = false) Long minDeposit,
            @RequestParam(required = false) Long maxDeposit,
            @RequestParam(required = false) Long minMonthlyRent,
            @RequestParam(required = false) Long maxMonthlyRent,
            @RequestParam(required = false) Integer minBuiltYear,
            @RequestParam(required = false) Integer maxBuiltYear,
            @RequestParam(required = false) Boolean hasElevator,
            @RequestParam(required = false) Boolean hasParking,
            @RequestParam(required = false) Integer roomCount,
            @RequestParam(required = false) Boolean petAllowed,
            @RequestParam(required = false) Boolean lhAvailable,
            @RequestParam(required = false) Double minArea,
            @RequestParam(required = false) Double maxArea,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "500") int limit
    ) {
        if (id != null) {
            try {
                Property p = propertyService.findById(id);
                if (p.getLat() == null || p.getLng() == null) return List.of();
                return List.of(PropertyMarkerDto.from(p));
            } catch (IllegalArgumentException e) {
                return List.of();
            }
        }

        Specification<Property> spec = Specification
                .where(PropertySpecs.regionEq(region))
                .and(PropertySpecs.dealTypeEq(dealType))
                .and(PropertySpecs.depositBetween(minDeposit, maxDeposit))
                .and(PropertySpecs.monthlyRentBetween(minMonthlyRent, maxMonthlyRent))
                .and(PropertySpecs.builtYearBetween(minBuiltYear, maxBuiltYear))
                .and(PropertySpecs.hasElevator(hasElevator))
                .and(PropertySpecs.hasParking(hasParking))
                .and(PropertySpecs.roomCountEq(roomCount))
                .and(PropertySpecs.petAllowed(petAllowed))
                .and(PropertySpecs.lhAvailable(lhAvailable))
                .and(PropertySpecs.areaBetween(minArea, maxArea))
                .and(PropertySpecs.keywordContains(keyword))
                .and((root, query, cb) -> cb.isNotNull(root.get("lat")))
                .and((root, query, cb) -> cb.isNotNull(root.get("lng")));

        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        return propertyService.findAll(spec, PageRequest.of(0, safeLimit))
                .getContent().stream()
                .map(PropertyMarkerDto::from)
                .toList();
    }

    @GetMapping("/validate-address")
    public Map<String, Object> validateAddress(@RequestParam String address) {
        Map<String, Object> result = new HashMap<>();
        try {
            var point = naverGeocodingClient.geocodeOrThrow(address);
            result.put("valid", true);
            result.put("message", "확인된 주소입니다.");
            result.put("lat", point.lat());
            result.put("lng", point.lng());
        } catch (IllegalArgumentException e) {
            result.put("valid", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    public record PropertyMarkerDto(
            Long id,
            String title,
            String address,
            Double lat,
            Double lng,
            PropertyStatus status,
            DealType dealType,
            String dealTypeLabel,
            String statusLabel,
            Long depositMan,
            Long monthlyRentMan,
            String tenantPhone,
            String ownerPhone
    ) {
        static PropertyMarkerDto from(Property p) {
            return new PropertyMarkerDto(
                    p.getId(),
                    p.getTitle(),
                    p.getLotAddress(),
                    p.getLat(),
                    p.getLng(),
                    p.getStatus(),
                    p.getDealType(),
                    p.getDealType() == null ? "" : p.getDealType().getLabel(),
                    p.getStatus() == null ? "" : p.getStatus().getLabel(),
                    p.getDepositMan(),
                    p.getMonthlyRentMan(),
                    p.getTenantPhone(),
                    p.getOwnerPhone()
            );
        }
    }
}
