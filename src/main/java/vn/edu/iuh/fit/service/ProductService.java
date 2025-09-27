package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.Product;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.request.UpsertProductRequest;
import vn.edu.iuh.fit.repository.ProductRepository;
import vn.edu.iuh.fit.repository.AdditionalServiceItemRepository;
import vn.edu.iuh.fit.repository.AdditionalServiceRepository;
import vn.edu.iuh.fit.repository.CouponDetailTermsRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    private final AdditionalServiceItemRepository additionalServiceItemRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final CouponDetailTermsRepository couponDetailTermsRepository;
    
    // Lấy tất cả sản phẩm với filter status
    public List<Product> getAllProducts(Boolean status) {
        log.info("Get all products with status filter: {}", status);
        if (status != null) {
            return productRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return productRepository.findAll(Sort.by("createdAt").descending());
    }

    // Lấy sản phẩm với phân trang
    public Page<Product> getAllProductsWithPagination(Boolean status, int page, int size) {
        log.info("Get products with pagination - status: {}, page: {}, size: {}", status, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        if (status != null) {
            return productRepository.findByStatus(status, pageable);
        }
        return productRepository.findAll(pageable);
    }

    // Tìm kiếm sản phẩm với filter
    public Page<Product> searchProducts(String name, Boolean status, int page, int size) {
        log.info("Search products with filters - name: {}, status: {}, page: {}, size: {}", name, status, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.findProductsWithFilters(name, status, pageable);
    }
    
    // Lấy sản phẩm theo trạng thái
    public List<Product> getProductsByStatus(Boolean status) {
        return productRepository.findByStatus(status);
    }
    
    // Lấy sản phẩm theo ID
    public Product getProductById(Integer id) {
        log.info("Get product by id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
    
    // Lấy sản phẩm theo SKU
    public Product getProductBySku(String sku) {
        log.info("Get product by sku: {}", sku);
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with sku: " + sku));
    }
    
    // Tìm sản phẩm theo tên
    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
    
    // Tạo mới sản phẩm với UpsertProductRequest
    public Product saveProduct(UpsertProductRequest request) {
        log.info("Creating new product with sku: {}", request.getSku());
        validateProductSku(request.getSku(), null);

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .unit(request.getUnit())
                .quantity(request.getQuantity())
                .thumbnail(request.getThumbnail())
                .status(request.getStatus())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());
        return savedProduct;
    }
    
    // Tạo mới sản phẩm (method cũ để backward compatibility)
    public Product createProduct(Product product) {
        validateProductSku(product.getSku(), null);
        return productRepository.save(product);
    }
    
    // Cập nhật sản phẩm với UpsertProductRequest
    public Product updateProduct(Integer id, UpsertProductRequest request) {
        log.info("Updating product with id: {}", id);
        
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Kiểm tra SKU đã tồn tại (ngoại trừ sản phẩm hiện tại)
        validateProductSku(request.getSku(), id);

        existingProduct.setSku(request.getSku());
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setUnit(request.getUnit());
        existingProduct.setQuantity(request.getQuantity());
        existingProduct.setThumbnail(request.getThumbnail());
        existingProduct.setStatus(request.getStatus());

        Product updatedProduct = productRepository.save(existingProduct);
        log.info("Product updated successfully with id: {}", updatedProduct.getId());
        return updatedProduct;
    }
    
    // Cập nhật sản phẩm (method cũ để backward compatibility)
    public Product updateProduct(Integer id, Product productData) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        validateProductSku(productData.getSku(), id);
        
        existingProduct.setSku(productData.getSku());
        existingProduct.setName(productData.getName());
        existingProduct.setDescription(productData.getDescription());
        existingProduct.setUnit(productData.getUnit());
        existingProduct.setQuantity(productData.getQuantity());
        existingProduct.setThumbnail(productData.getThumbnail());
        existingProduct.setStatus(productData.getStatus());
        
        return productRepository.save(existingProduct);
    }
    
    // Xóa sản phẩm
    public void deleteProduct(Integer id) {
        log.info("Deleting product with id: {}", id);
        
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Kiểm tra xem sản phẩm có đang được sử dụng trong additional service không
        if (additionalServiceItemRepository.existsByProductId(id)) {
            throw new IllegalArgumentException("Cannot delete product. It is being used in additional services.");
        }
        
        productRepository.delete(product);
        log.info("Product deleted successfully with id: {}", id);
    }
    
    // Thay đổi trạng thái sản phẩm
    public Product toggleProductStatus(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        
        // Nếu đang chuyển từ active sang inactive, cần kiểm tra ràng buộc
        if (product.getStatus() && !checkProductConstraints(id)) {
            throw new IllegalArgumentException("Cannot deactivate product. It is being used in active promotions or additional services.");
        }
        
        product.setStatus(!product.getStatus());
        return productRepository.save(product);
    }
    
    // Kiểm tra xem có thể vô hiệu hóa sản phẩm không
    private boolean checkProductConstraints(Integer productId) {
        // Kiểm tra sản phẩm có đang được dùng trong additional service đang hoạt động
        if (additionalServiceItemRepository.existsByProductIdAndAdditionalServiceStatusTrue(productId)) {
            log.warn("Product {} is being used in active additional services", productId);
            return false;
        }
        
        // Kiểm tra sản phẩm có đang được dùng trong coupon detail đang hoạt động
        if (couponDetailTermsRepository.existsByGiftServiceIdAndEnabledTrueAndCouponStatusTrue(productId)) {
            log.warn("Product {} is being used in active coupon promotions", productId);
            return false;
        }
        
        return true;
    }
    
    // Public method để kiểm tra có thể vô hiệu hóa sản phẩm không
    public boolean canDeactivateProduct(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        // Nếu sản phẩm đã inactive thì luôn return true
        if (!product.getStatus()) {
            return true;
        }
        
        return checkProductConstraints(productId);
    }

    // Lấy tất cả sản phẩm active
    public List<Product> getActiveProducts() {
        log.info("Get all active products");
        return productRepository.findByStatusTrue();
    }

    public boolean existsBySku(String sku) {
        return productRepository.existsBySku(sku);
    }

    public boolean existsBySkuAndNotId(String sku, Integer id) {
        return productRepository.existsBySkuAndIdNot(sku, id);
    }
    
    // Validate SKU
    private void validateProductSku(String sku, Integer excludeId) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("Product SKU cannot be empty");
        }
        
        boolean exists = excludeId != null 
            ? productRepository.existsBySkuAndIdNot(sku, excludeId)
            : productRepository.existsBySku(sku);
            
        if (exists) {
            throw new IllegalArgumentException("Product SKU already exists: " + sku);
        }
    }
}