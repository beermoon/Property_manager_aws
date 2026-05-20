package kr.co.choi.property_manager.controller;

import kr.co.choi.property_manager.controller.dto.PropertyCreateRequest;
import kr.co.choi.property_manager.domain.DealType;
import kr.co.choi.property_manager.domain.Property;
import kr.co.choi.property_manager.domain.PropertyStatus;
import kr.co.choi.property_manager.repository.specs.PropertySpecs;
import kr.co.choi.property_manager.service.PropertyService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 매물 화면(MVC)을 담당하는 컨트롤러.
 *
 * <p>비즈니스 로직은 모두 {@link PropertyService}에 위임하고, 이 클래스는
 * HTTP 요청을 받아 Service를 호출하고 View로 분기하는 얇은 layer로만 존재한다.
 */
@Controller
@RequestMapping("/properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    // 1) 목록
    @GetMapping
    public String list(
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

            @RequestParam(required = false) String expiry,
            Model model
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

        model.addAttribute("properties", propertyService.findAll(spec));

        // 폼 값 유지용
        model.addAttribute("keyword", keyword);
        model.addAttribute("region", region);
        model.addAttribute("dealType", dealType);
        model.addAttribute("status", status);
        model.addAttribute("minDeposit", minDeposit);
        model.addAttribute("maxDeposit", maxDeposit);
        model.addAttribute("minMonthlyRent", minMonthlyRent);
        model.addAttribute("maxMonthlyRent", maxMonthlyRent);
        model.addAttribute("minBuiltYear", minBuiltYear);
        model.addAttribute("maxBuiltYear", maxBuiltYear);
        model.addAttribute("minArea", minArea);
        model.addAttribute("maxArea", maxArea);
        model.addAttribute("roomCount", roomCount);
        model.addAttribute("hasElevator", hasElevator);
        model.addAttribute("hasParking", hasParking);
        model.addAttribute("petAllowed", petAllowed);
        model.addAttribute("lhAvailable", lhAvailable);
        model.addAttribute("expiry", expiry);

        return "properties/list";
    }

    // 2) 등록 폼
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("dealTypes", DealType.values());
        model.addAttribute("statuses", PropertyStatus.values());
        return "properties/new";
    }

    // 3) 등록 처리
    @PostMapping
    public String create(@ModelAttribute("form") PropertyCreateRequest request,
                         @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                         Model model) {
        try {
            Long id = propertyService.create(request, photos);
            return "redirect:/properties/" + id;
        } catch (Exception e) {
            return renderFormWithError("properties/new", e, request, model);
        }
    }

    // 4) 상세 (메모 목록 포함)
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Property property = propertyService.findById(id);
        model.addAttribute("property", property);
        model.addAttribute("memos", propertyService.findMemosByProperty(id));
        return "properties/detail";
    }

    // 5) 메모 추가
    @PostMapping("/{id}/memos")
    public String addMemo(@PathVariable Long id, @RequestParam String content) {
        propertyService.addMemo(id, content);
        return "redirect:/properties/" + id;
    }

    // 6) 메모 삭제
    @PostMapping("/{propertyId}/memos/{memoId}/delete")
    public String deleteMemo(@PathVariable Long propertyId, @PathVariable Long memoId) {
        propertyService.deleteMemo(propertyId, memoId);
        return "redirect:/properties/" + propertyId;
    }

    @GetMapping("/{id}/memos/new")
    public String newMemoForm(@PathVariable Long id, Model model) {
        model.addAttribute("propertyId", id);
        return "MemoNew";
    }

    // 7) 매물 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        propertyService.delete(id);
        return "redirect:/properties";
    }

    // 8) 수정 폼
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Property property = propertyService.findById(id);

        model.addAttribute("property", property);
        model.addAttribute("form", PropertyCreateRequest.from(property));   // ← 정적 팩토리로 한 줄
        model.addAttribute("propertyId", id);
        model.addAttribute("dealTypes", DealType.values());
        return "properties/edit";
    }

    // 9) 수정 처리
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("form") PropertyCreateRequest request,
                         @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                         Model model) {
        try {
            propertyService.update(id, request, photos);
            return "redirect:/properties/" + id;
        } catch (Exception e) {
            Property property = propertyService.findById(id);
            model.addAttribute("property", property);
            model.addAttribute("propertyId", id);
            return renderFormWithError("properties/edit", e, request, model);
        }
    }

    // 10) 사진 삭제
    @PostMapping("/{propertyId}/photos/{photoId}/delete")
    public String deletePhoto(@PathVariable Long propertyId, @PathVariable Long photoId) {
        propertyService.deletePhoto(propertyId, photoId);
        return "redirect:/properties/" + propertyId + "/edit";
    }

    // ============================================================
    //   내부 헬퍼
    // ============================================================

    /**
     * 폼 처리 중 예외가 발생했을 때 입력값을 유지하면서 폼을 다시 렌더한다.
     * create / update 양쪽에서 공통으로 쓰는 패턴이라 추출했다.
     */
    private String renderFormWithError(String viewName, Exception e,
                                       PropertyCreateRequest request, Model model) {
        model.addAttribute("error", e.getMessage());
        model.addAttribute("form", request);
        model.addAttribute("dealTypes", DealType.values());
        model.addAttribute("statuses", PropertyStatus.values());
        return viewName;
    }
}
