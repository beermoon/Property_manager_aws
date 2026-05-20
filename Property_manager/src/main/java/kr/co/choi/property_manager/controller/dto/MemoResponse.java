package kr.co.choi.property_manager.controller.dto;

import kr.co.choi.property_manager.domain.Memo;

import java.time.LocalDateTime;

/**
 * 메모 응답 DTO.
 *
 * <p>{@link Memo} 엔티티의 {@code property} 역참조를 노출하지 않기 위함이다.
 * 엔티티를 그대로 직렬화하면 property → memos → property... 무한 루프에 빠지거나
 * lazy 컬렉션이 펼쳐져 N+1을 유발한다.
 */
public record MemoResponse(
        Long id,
        Long propertyId,
        String content,
        LocalDateTime createdAt
) {
    public static MemoResponse from(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getProperty() == null ? null : memo.getProperty().getId(),
                memo.getContent(),
                memo.getCreatedAt()
        );
    }
}
