package kr.co.choi.property_manager.repository;

import kr.co.choi.property_manager.domain.PropertyPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyPhotoRepository extends JpaRepository<PropertyPhoto, Long> {
    List<PropertyPhoto> findByPropertyIdOrderByIdDesc(Long propertyId);

}
