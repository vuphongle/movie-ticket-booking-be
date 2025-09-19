package vn.edu.iuh.fit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.Product;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    
    // Tìm sản phẩm theo SKU
    Optional<Product> findBySku(String sku);
    
    // Tìm sản phẩm theo trạng thái
    List<Product> findByStatus(Boolean status);
    
    // Tìm sản phẩm theo trạng thái và sắp xếp theo ngày tạo
    List<Product> findByStatusOrderByCreatedAtDesc(Boolean status);
    
    // Tìm sản phẩm theo trạng thái với phân trang
    Page<Product> findByStatus(Boolean status, Pageable pageable);
    
    // Tìm sản phẩm theo tên (không phân biệt hoa thường)
    @Query("SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Product> findByNameContainingIgnoreCase(@Param("name") String name);
    
    // Tìm kiếm sản phẩm với filter và phân trang
    @Query("SELECT p FROM Product p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:status IS NULL OR p.status = :status)")
    Page<Product> findProductsWithFilters(@Param("name") String name, 
                                        @Param("status") Boolean status, 
                                        Pageable pageable);
    
    // Kiểm tra SKU đã tồn tại
    boolean existsBySku(String sku);
    
    // Kiểm tra SKU đã tồn tại (trừ ID hiện tại)
    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku AND p.id != :id")
    boolean existsBySkuAndIdNot(@Param("sku") String sku, @Param("id") Integer id);
    
    // Tìm tất cả sản phẩm active
    List<Product> findByStatusTrue();
}