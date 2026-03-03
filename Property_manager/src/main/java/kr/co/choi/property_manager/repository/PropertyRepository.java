package kr.co.choi.property_manager.repository;

import kr.co.choi.property_manager.domain.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    @Query("""
        select p from Property p
        where lower(p.title) like lower(concat('%', :keyword, '%'))
           or lower(p.address) like lower(concat('%', :keyword, '%'))
    """)
    List<Property> search(@Param("keyword") String keyword);


}
