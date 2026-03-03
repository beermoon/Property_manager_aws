package kr.co.choi.property_manager.controller;


import kr.co.choi.property_manager.domain.DealType;
import kr.co.choi.property_manager.domain.Property;
import kr.co.choi.property_manager.domain.PropertyStatus;
import kr.co.choi.property_manager.domain.PropertyType;
import kr.co.choi.property_manager.infra.NaverGeocodingClient;
import kr.co.choi.property_manager.repository.PropertyRepository;
import kr.co.choi.property_manager.repository.specs.PropertySpecs;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import kr.co.choi.property_manager.infra.NaverGeocodingClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PropertyApiController {

    private final PropertyRepository propertyRepository;
    private final NaverGeocodingClient naverGeocodingClient;

    public PropertyApiController(PropertyRepository propertyRepository,
                                 NaverGeocodingClient naverGeocodingClient) {
        this.propertyRepository = propertyRepository;
        this.naverGeocodingClient = naverGeocodingClient;
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


    @GetMapping("/properties")
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
            return propertyRepository.findById(id)
                    .filter(p -> p.getLat() != null && p.getLng() != null)
                    .map(p -> java.util.List.of(PropertyMarkerDto.from(p)))
                    .orElseGet(java.util.List::of);
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
                // ✅ 좌표 없는 매물은 지도 마커 불가 → 제외
                .and((root, query, cb) -> cb.isNotNull(root.get("lat")))
                .and((root, query, cb) -> cb.isNotNull(root.get("lng")));

        int safeLimit = Math.min(Math.max(limit, 1), 2000); // 1~2000 방어
        var page = propertyRepository.findAll(spec, PageRequest.of(0, safeLimit));

        return page.getContent().stream()
                .map(PropertyMarkerDto::from)
                .toList();
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




