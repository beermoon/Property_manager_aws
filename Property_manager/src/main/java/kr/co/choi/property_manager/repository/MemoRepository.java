package kr.co.choi.property_manager.repository;

import kr.co.choi.property_manager.domain.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo,Long> {
    List<Memo> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);

    void deleteByIdAndPropertyId(Long id, Long propertyId);

}
