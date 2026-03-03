package kr.co.choi.property_manager.controller;

import kr.co.choi.property_manager.controller.dto.PropertyCreateRequest;
import kr.co.choi.property_manager.domain.*;
import kr.co.choi.property_manager.infra.NaverGeocodingClient;
import kr.co.choi.property_manager.repository.MemoRepository;
import kr.co.choi.property_manager.repository.PropertyPhotoRepository;
import kr.co.choi.property_manager.repository.PropertyRepository;
import kr.co.choi.property_manager.repository.specs.PropertySpecs;
import kr.co.choi.property_manager.service.FileStorageService;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/properties")
public class PropertyController {

    private final PropertyRepository propertyRepository;
    private final MemoRepository memoRepository;
    private final NaverGeocodingClient naverGeocodingClient;
    private final FileStorageService fileStorageService;
    private final PropertyPhotoRepository propertyPhotoRepository;

    public PropertyController(PropertyRepository propertyRepository,
                                MemoRepository memoRepository,
                            NaverGeocodingClient naverGeocodingClient,
                            FileStorageService fileStorageService,
                              PropertyPhotoRepository propertyPhotoRepository) {
        this.propertyRepository = propertyRepository;
        this.memoRepository = memoRepository;
        this.naverGeocodingClient = naverGeocodingClient;
        this.fileStorageService = fileStorageService;
        this.propertyPhotoRepository = propertyPhotoRepository;
    }

    // 1) 리스트
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
        var spec = Specification.where(PropertySpecs.keywordContains(keyword))
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

        model.addAttribute("properties", propertyRepository.findAll(spec));

        // 폼 값 유지용(선택)
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

    @PostMapping
    public String create(@ModelAttribute("form") PropertyCreateRequest request,
                         @RequestParam(value = "photos", required = false) java.util.List<MultipartFile> photos,
                         Model model) {



        try {

            if (request.getLotAddress() == null || request.getLotAddress().isBlank()) {
                throw new IllegalArgumentException("지번 주소는 필수입니다.");
            }

            // 1) 지오코딩 (지번주소 기준)
            var point = naverGeocodingClient.geocodeOrThrow(request.getLotAddress());

            // 2) 엔티티 생성 + 값 세팅
            Property property = new Property();
            property.updateAll(request);

            // 3) 좌표 저장
            property.updateLocation(point.lat(), point.lng());

            // 4) 저장
            propertyRepository.save(property);

            // 5) 사진 저장
            if (photos != null) {
                for (MultipartFile file : photos) {
                    if (file == null || file.isEmpty()) continue;

                    var stored = fileStorageService.store(file);
                    var photo = new PropertyPhoto(
                            property,
                            stored.originalName(),
                            stored.storedName(),
                            stored.url()
                    );

                    property.addPhoto(photo);
                    propertyPhotoRepository.save(photo);
                }
            }

            return "redirect:/properties/" + property.getId();

        } catch (Exception e) {
            e.printStackTrace();

            model.addAttribute("error", e.getMessage());

            // ✅ 입력값 유지 핵심
            model.addAttribute("form", request);

            // ✅ select 옵션 유지 (new.html에서 쓰는 것들 전부)
            model.addAttribute("dealTypes", DealType.values());
            model.addAttribute("statuses", PropertyStatus.values()); // 쓰고 있으면 꼭

            return "properties/new"; // ✅ redirect 하지 말고 그대로 렌더링
        }
    }

    // 4) 상세 (+ 메모 목록)
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Property not found : " + id));

        model.addAttribute("property",property);
        model.addAttribute("memos", memoRepository.findByPropertyIdOrderByCreatedAtDesc(id));
        return "properties/detail";
    }

    // 메모 추가
    @PostMapping("/{id}/memos")
    public String addMemo(@PathVariable Long id,
                          @RequestParam String content) {

        Property property = propertyRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("Property not found : " + id));

        Memo memo = new Memo(content);
        // 연관관계 설정 (Property의 addMemo 사용)
        property.addMemo(memo);      // 연관관계 세팅
        memoRepository.save(memo);   // ✅ 메모를 직접 저장 (확실)

        return "redirect:/properties/" + id;
    }

    // 메모 삭제
    @PostMapping("/{propertyId}/memos/{memoId}/delete")
    public String deleteMemo(@PathVariable Long propertyId,
                             @PathVariable Long memoId) {

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("Memo not found: " + memoId));

        if (memo.getProperty() == null || !memo.getProperty().getId().equals(propertyId)) {
            throw new IllegalArgumentException("잘못된 요청입니다");
        }

        memoRepository.delete(memo);

        return "redirect:/properties/" + propertyId;

    }


    @GetMapping("/{id}/memos/new")
    public String newMemoForm(@PathVariable Long id, Model model) {
        model.addAttribute("propertyId",id);
        return "MemoNew";
    }



    // 5) 삭제
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        propertyRepository.deleteById(id);
        return "redirect:/properties";
    }

    // 6) 수정

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found : " + id));

        model.addAttribute("property",property);

        // ✅ Property -> DTO 변환 (폼에 값 채우기)
        PropertyCreateRequest form = new PropertyCreateRequest();
        form.setTitle(property.getTitle());
        form.setRegion(property.getRegion());
        form.setBuildingName(property.getBuildingName());
        form.setAddress(property.getAddress());
        form.setLotAddress(property.getLotAddress());
        form.setUnitNumber(property.getUnitNumber());
        form.setBuiltYear(property.getBuiltYear());
        form.setArea(property.getArea());

        form.setDealType(property.getDealType());
        form.setDeposit(property.getDepositMan());          // ✅ 폼은 만원 단위
        form.setMonthlyRent(property.getMonthlyRentMan());
        form.setManagementFee(property.getManagementFeeMan());
        form.setStatus(property.getStatus());
        form.setExpiry(property.getExpiry());

        form.setHasElevator(property.getHasElevator());
        form.setHasParking(property.getHasParking());
        form.setRoomCount(property.getRoomCount());
        form.setPetAllowed(property.getPetAllowed());
        form.setLhAvailable(property.getLhAvailable());

        form.setEntrancePassword(property.getEntrancePassword());
        form.setHousePassword(property.getHousePassword());
        form.setTenantPhone(property.getTenantPhone());
        form.setOwnerPhone(property.getOwnerPhone());



        model.addAttribute("propertyId", id);
        model.addAttribute("form", form);
        model.addAttribute("dealTypes", DealType.values());
        return "properties/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("form") PropertyCreateRequest request,
                         @RequestParam(value="photos", required=false) java.util.List<org.springframework.web.multipart.MultipartFile> photos,
                         Model model) {
        try {
            Property property = propertyRepository.findById(id)
                    .orElseThrow(()-> new IllegalArgumentException("Property not found : " + id));

            String newLot = request.getLotAddress();
            String oldLot = property.getLotAddress();

            if (newLot != null && !newLot.equals(oldLot)) {
                var point = naverGeocodingClient.geocodeOrThrow(newLot);
                property.updateLocation(point.lat(), point.lng());
            }

            // 나머지 값 갱신
            property.updateAll(request);

            propertyRepository.save(property);

            if (photos != null) {
                for(var file : photos) {
                    if (file == null || file.isEmpty()) continue;

                    var stored = fileStorageService.store(file);

                    var photo = new PropertyPhoto(
                            property,
                            stored.originalName(),
                            stored.storedName(),
                            stored.url()

                    );

                    property.addPhoto(photo);
                    propertyPhotoRepository.save(photo);
                }
            }


            return "redirect:/properties/" + id;

        } catch (Exception e) {
            e.printStackTrace();

            Property property = propertyRepository.findById(id).orElse(null);

            model.addAttribute("error", e.getMessage());
            model.addAttribute("property", property);
            model.addAttribute("form", request);
            model.addAttribute("dealTypes", DealType.values());
            model.addAttribute("propertyId", id);



            return "properties/edit";

        }
    }

    // 사진 삭제 엔드포인트
    @PostMapping("/{propertyId}/photos/{photoId}/delete")
    public String deletePhoto(@PathVariable Long propertyId,
                              @PathVariable Long photoId) {

        PropertyPhoto photo = propertyPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found : " + photoId));

        // 다른 매물의 사진을 지우는 실수 방지
        if (!photo.getProperty().getId().equals(propertyId)) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        // 1) DB 삭제
        propertyPhotoRepository.delete(photo);

         //2) ( 실제 파일도 삭제하고 싶으면 아래 추가
         fileStorageService.deleteByUrl(photo.getUrl());

        return "redirect:/properties/" + propertyId + "/edit";
    }







}
