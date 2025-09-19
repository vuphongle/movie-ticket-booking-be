package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.AdditionalServiceItem;

import java.util.List;

@Repository
public interface AdditionalServiceItemRepository extends JpaRepository<AdditionalServiceItem, Integer> {
    
    // Tìm các item theo additional service id
    List<AdditionalServiceItem> findByAdditionalServiceId(Integer additionalServiceId);
    
    // Tìm các item theo product id
    List<AdditionalServiceItem> findByProductId(Integer productId);
    
    // Xóa tất cả items của một additional service
    void deleteByAdditionalServiceId(Integer additionalServiceId);
    
    // Tìm tất cả items của additional service với thông tin product
    @Query("SELECT asi FROM AdditionalServiceItem asi " +
           "JOIN FETCH asi.product p " +
           "WHERE asi.additionalService.id = :serviceId")
    List<AdditionalServiceItem> findByAdditionalServiceIdWithProduct(@Param("serviceId") Integer serviceId);
    
    // Kiểm tra xem product có được sử dụng trong additional service nào không
    @Query("SELECT COUNT(asi) > 0 FROM AdditionalServiceItem asi WHERE asi.product.id = :productId")
    boolean existsByProductId(@Param("productId") Integer productId);
}