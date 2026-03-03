package kr.co.choi.property_manager.repository.specs;

import jakarta.persistence.criteria.Predicate;
import kr.co.choi.property_manager.domain.DealType;
import kr.co.choi.property_manager.domain.Property;
import kr.co.choi.property_manager.domain.PropertyStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class PropertySpecs {

    public static Specification<Property> keywordContains(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return cb.conjunction();
            String like = "%" + keyword.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), like),
                    cb.like(cb.lower(root.get("lotAddress")), like),
                    cb.like(cb.lower(root.get("buildingName")), like)
            );
        };
    }

    public static Specification<Property> statusEq(PropertyStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    // 지도 화면 범위 필터 (선택)
    public static Specification<Property> inBounds(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        return (root, query, cb) -> {
            if (minLat == null || maxLat == null || minLng == null || maxLng == null) return cb.conjunction();
            return cb.and(
                    cb.between(root.get("lat"), minLat, maxLat),
                    cb.between(root.get("lng"), minLng, maxLng)
            );
        };
    }

    // 보증금
    public static Specification<Property> depositBetween(Long min, Long max) {
        return (root, query,cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (min != null) ps.add(cb.greaterThanOrEqualTo(root.get("deposit"), min * 10_000));
            if (max != null) ps.add(cb.lessThanOrEqualTo(root.get("deposit"), max * 10_000));
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));

        };
    }




    // 준공연도
    public static Specification<Property> builtYearBetween(Integer min, Integer max) {
        return (root, query,cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (min != null) ps.add(cb.greaterThanOrEqualTo(root.get("builtYear"), min ));
            if (max != null) ps.add(cb.lessThanOrEqualTo(root.get("builtYear"), max ));
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));

        };
    }

    // boolean 필터들
    public static Specification<Property> hasElevator(Boolean value) {
        return (root, query, cb) ->
                value == null ? cb.conjunction() : cb.equal(root.get("hasElevator"),value);
    }

    public static Specification<Property> hasParking(Boolean value) {
        return (root, query, cb) ->
                value == null ? cb.conjunction() : cb.equal(root.get("hasParking"),value);
    }

    public static Specification<Property> petAllowed(Boolean value) {
        return (root, query, cb) ->
                value == null ? cb.conjunction() : cb.equal(root.get("petAllowed"),value);
    }

    public static Specification<Property> lhAvailable(Boolean value) {
        return (root, query, cb) ->
                value == null ? cb.conjunction() : cb.equal(root.get("lhAvailable"),value);
    }

    public static Specification<Property> roomCountEq(Integer count) {
        return (root, query, cb) ->
                count == null ? cb.conjunction() : cb.equal(root.get("roomCount"),count);
    }

    public static Specification<Property> regionEq(String region) {
        return (root,query,cb) ->
                (region == null || region.isBlank())
                ? cb.conjunction()
                        : cb.equal(root.get("region"),region);
    }

    public static Specification<Property> dealTypeEq(DealType dealType) {
        return (root,  query, cb) ->
                (dealType == null)
                ? cb.conjunction()
                : cb.equal(root.get("dealType"), dealType);
    }

    // 월세
    public static Specification<Property> monthlyRentBetween(Long minMan, Long maxMan) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (minMan != null) ps.add(cb.greaterThanOrEqualTo(root.get("monthlyRent"), minMan * 10_000));
            if (maxMan != null) ps.add(cb.lessThanOrEqualTo(root.get("monthlyRent"), maxMan * 10_000));
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));
        };
    }



    public static Specification<Property> areaBetween(Double min, Double max) {
        return (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (min != null) ps.add(cb.greaterThanOrEqualTo(root.get("area"), min));
            if (max != null) ps.add(cb.lessThanOrEqualTo(root.get("area"), max));
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));
        };
    }


    // 만기
    public static Specification<Property> expiryContains(String expiry) {
        return (root, query, cb) -> {
            if (expiry == null || expiry.isBlank()) return cb.conjunction();
            String like = "%" + expiry.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("expiry")), like);
        };
    }


}