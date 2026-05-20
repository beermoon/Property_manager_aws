package kr.co.choi.property_manager.service;

import kr.co.choi.property_manager.controller.dto.PropertyCreateRequest;
import kr.co.choi.property_manager.domain.Memo;
import kr.co.choi.property_manager.domain.Property;
import kr.co.choi.property_manager.domain.PropertyPhoto;
import kr.co.choi.property_manager.infra.NaverGeocodingClient;
import kr.co.choi.property_manager.repository.MemoRepository;
import kr.co.choi.property_manager.repository.PropertyPhotoRepository;
import kr.co.choi.property_manager.repository.PropertyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * 매물(Property) 도메인의 비즈니스 로직을 담당하는 서비스.
 *
 * <p>컨트롤러는 이 서비스의 메서드만 호출하면 되도록 설계되어 있어,
 * MVC 컨트롤러와 REST 컨트롤러가 같은 비즈니스 로직을 공유할 수 있습니다.
 *
 * <p>트랜잭션 정책:
 * <ul>
 *   <li>클래스 레벨 {@code @Transactional(readOnly = true)} — 기본은 읽기 전용</li>
 *   <li>변경이 일어나는 메서드에만 {@code @Transactional} — 쓰기 트랜잭션</li>
 * </ul>
 * 이 패턴은 "기본은 안전(읽기), 쓰기는 명시"라서 실수로 쓰기 메서드를 읽기로
 * 만드는 사고를 막아줍니다.
 */
@Service
@Transactional(readOnly = true)
public class PropertyService {

    private static final Logger log = LoggerFactory.getLogger(PropertyService.class);

    private final PropertyRepository propertyRepository;
    private final PropertyPhotoRepository propertyPhotoRepository;
    private final MemoRepository memoRepository;
    private final NaverGeocodingClient naverGeocodingClient;
    private final FileStorageService fileStorageService;

    public PropertyService(PropertyRepository propertyRepository,
                           PropertyPhotoRepository propertyPhotoRepository,
                           MemoRepository memoRepository,
                           NaverGeocodingClient naverGeocodingClient,
                           FileStorageService fileStorageService) {
        this.propertyRepository = propertyRepository;
        this.propertyPhotoRepository = propertyPhotoRepository;
        this.memoRepository = memoRepository;
        this.naverGeocodingClient = naverGeocodingClient;
        this.fileStorageService = fileStorageService;
    }

    // ============================================================
    //   매물 CRUD
    // ============================================================

    /**
     * 매물을 생성하고, 옵션으로 사진들도 함께 저장한다.
     *
     * <p>흐름: 입력 검증 → 지오코딩 → 엔티티 생성/저장 → 사진 저장.
     * 전체가 하나의 트랜잭션 안에서 일어나므로, 중간에 예외가 터지면
     * DB는 롤백된다. (다만 디스크에 이미 쓰인 파일은 자동 롤백되지 않는다.)
     *
     * @return 생성된 매물의 ID
     */
    @Transactional
    public Long create(PropertyCreateRequest request, List<MultipartFile> photos) {
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

        // 4) 매물 저장
        propertyRepository.save(property);

        // 5) 사진 저장
        savePhotos(property, photos);

        return property.getId();
    }

    /**
     * 매물 정보를 갱신한다. 지번 주소가 바뀐 경우에만 좌표를 다시 지오코딩한다.
     * 사진은 "추가만" — 기존 사진을 지우려면 별도 deletePhoto를 호출한다.
     */
    @Transactional
    public void update(Long id, PropertyCreateRequest request, List<MultipartFile> photos) {
        Property property = findById(id);

        String newLot = request.getLotAddress();
        String oldLot = property.getLotAddress();

        if (newLot != null && !newLot.equals(oldLot)) {
            var point = naverGeocodingClient.geocodeOrThrow(newLot);
            property.updateLocation(point.lat(), point.lng());
        }

        property.updateAll(request);
        propertyRepository.save(property);

        savePhotos(property, photos);
    }

    /**
     * 매물을 삭제한다. 매물에 딸린 사진/메모는 JPA cascade로 함께 지워지지만,
     * 디스크의 실제 사진 파일은 JPA가 모르므로 여기서 명시적으로 지운다.
     */
    @Transactional
    public void delete(Long id) {
        Property property = findById(id);

        // 디스크 파일 먼저 정리 (DB 삭제 후 property에서 photos를 못 꺼내므로)
        for (PropertyPhoto photo : property.getPhotos()) {
            fileStorageService.deleteByUrl(photo.getUrl());
        }

        propertyRepository.delete(property);
    }

    /**
     * ID로 매물을 조회하되, 없으면 IllegalArgumentException을 던진다.
     * "있다고 가정"하는 호출 지점에서 분기를 줄이려고 만든 헬퍼.
     */
    public Property findById(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Property not found : " + id));
    }

    /**
     * Specification 기반 전체 조회 (목록 화면용).
     */
    public List<Property> findAll(Specification<Property> spec) {
        return propertyRepository.findAll(spec);
    }

    /**
     * Specification + 페이징 조회 (지도 마커용 — 상한 보호).
     */
    public Page<Property> findAll(Specification<Property> spec, Pageable pageable) {
        return propertyRepository.findAll(spec, pageable);
    }

    // ============================================================
    //   메모
    // ============================================================

    /**
     * 특정 매물에 메모를 추가한다.
     */
    @Transactional
    public void addMemo(Long propertyId, String content) {
        Property property = findById(propertyId);
        Memo memo = new Memo(content);
        property.addMemo(memo);          // 연관관계 세팅 (양방향 동기화)
        memoRepository.save(memo);
    }

    /**
     * 메모를 삭제한다. 메모가 정말 그 매물에 속한 것인지 검사하여
     * URL 조작으로 다른 매물의 메모를 지우는 사고를 막는다.
     */
    @Transactional
    public void deleteMemo(Long propertyId, Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("Memo not found: " + memoId));

        if (memo.getProperty() == null || !memo.getProperty().getId().equals(propertyId)) {
            throw new IllegalArgumentException("잘못된 요청입니다");
        }

        memoRepository.delete(memo);
    }

    /**
     * 매물에 딸린 메모 목록(최신순).
     */
    public List<Memo> findMemosByProperty(Long propertyId) {
        return memoRepository.findByPropertyIdOrderByCreatedAtDesc(propertyId);
    }

    // ============================================================
    //   사진
    // ============================================================

    /**
     * 매물에 사진 한 장을 삭제한다. DB 레코드와 디스크 파일을 모두 정리.
     */
    @Transactional
    public void deletePhoto(Long propertyId, Long photoId) {
        PropertyPhoto photo = propertyPhotoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found : " + photoId));

        if (!photo.getProperty().getId().equals(propertyId)) {
            throw new IllegalArgumentException("잘못된 요청입니다.");
        }

        propertyPhotoRepository.delete(photo);
        fileStorageService.deleteByUrl(photo.getUrl());
    }

    // ============================================================
    //   내부 헬퍼
    // ============================================================

    /**
     * 사진 리스트를 저장한다. null/empty는 무시.
     * <p>{@link IOException}을 {@link UncheckedIOException}으로 감싸서
     * 트랜잭션 롤백이 트리거되도록 한다(Checked 예외는 기본적으로 롤백되지 않음).
     */
    private void savePhotos(Property property, List<MultipartFile> photos) {
        if (photos == null || photos.isEmpty()) return;

        for (MultipartFile file : photos) {
            if (file == null || file.isEmpty()) continue;
            try {
                var stored = fileStorageService.store(file);
                var photo = new PropertyPhoto(
                        property,
                        stored.originalName(),
                        stored.storedName(),
                        stored.url()
                );
                property.addPhoto(photo);
                propertyPhotoRepository.save(photo);
            } catch (IOException e) {
                log.error("사진 저장 실패: propertyId={}, original={}", property.getId(), file.getOriginalFilename(), e);
                throw new UncheckedIOException("사진 저장에 실패했습니다.", e);
            }
        }
    }
}
