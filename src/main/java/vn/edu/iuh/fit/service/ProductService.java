package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.Product;
import vn.edu.iuh.fit.repository.ProductRepository;
import vn.edu.iuh.fit.repository.AdditionalServiceItemRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {
    
    private final ProductRepository productRepository;
    private final AdditionalServiceItemRepository additionalServiceItemRepository;
    
    // Lấy tất cả sản phẩm
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    // Lấy sản phẩm theo trạng thái
    public List<Product> getProductsByStatus(Boolean status) {
        return productRepository.findByStatus(status);
    }
    
    // Lấy sản phẩm theo ID
    public Optional<Product> getProductById(Integer id) {
        return productRepository.findById(id);
    }
    
    // Lấy sản phẩm theo SKU
    public Optional<Product> getProductBySku(String sku) {
        return productRepository.findBySku(sku);
    }
    
    // Tìm sản phẩm theo tên
    public List<Product> searchProductsByName(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }
    
    // Tạo mới sản phẩm
    public Product createProduct(Product product) {
        validateProductSku(product.getSku(), null);
        return productRepository.save(product);
    }
    
    // Cập nhật sản phẩm
    public Product updateProduct(Integer id, Product productData) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        validateProductSku(productData.getSku(), id);
        
        existingProduct.setSku(productData.getSku());
        existingProduct.setName(productData.getName());
        existingProduct.setDescription(productData.getDescription());
        existingProduct.setUnit(productData.getUnit());
        existingProduct.setThumbnail(productData.getThumbnail());
        existingProduct.setStatus(productData.getStatus());
        
        return productRepository.save(existingProduct);
    }
    
    // Xóa sản phẩm
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        // Kiểm tra xem sản phẩm có đang được sử dụng trong additional service không
        if (additionalServiceItemRepository.existsByProductId(id)) {
            throw new RuntimeException("Cannot delete product. It is being used in additional services.");
        }
        
        productRepository.delete(product);
    }
    
    // Thay đổi trạng thái sản phẩm
    public Product toggleProductStatus(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
        
        product.setStatus(!product.getStatus());
        return productRepository.save(product);
    }
    
    // Validate SKU
    private void validateProductSku(String sku, Integer excludeId) {
        if (sku == null || sku.trim().isEmpty()) {
            throw new RuntimeException("Product SKU cannot be empty");
        }
        
        boolean exists = excludeId != null 
            ? productRepository.existsBySkuAndIdNot(sku, excludeId)
            : productRepository.existsBySku(sku);
            
        if (exists) {
            throw new RuntimeException("Product SKU already exists: " + sku);
        }
    }
}