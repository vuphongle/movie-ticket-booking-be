package vn.edu.iuh.fit.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.iuh.fit.entity.Product;
import vn.edu.iuh.fit.model.request.UpsertProductRequest;
import vn.edu.iuh.fit.service.ProductService;
import vn.edu.iuh.fit.service.PricingService;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("api")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ProductController {
    
    private final ProductService productService;
    private final PricingService pricingService;

    // ========== PUBLIC ENDPOINTS ==========
    
    @GetMapping("/public/products")
    public ResponseEntity<?> getPublicProducts(@RequestParam(required = false) Boolean status) {
        return ResponseEntity.ok(productService.getAllProducts(status));
    }

    @GetMapping("/public/products/active")
    public ResponseEntity<?> getActiveProducts() {
        return ResponseEntity.ok(productService.getActiveProducts());
    }

    @GetMapping("/public/products/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/public/products/sku/{sku}")
    public ResponseEntity<?> getProductBySku(@PathVariable String sku) {
        return ResponseEntity.ok(productService.getProductBySku(sku));
    }

    @GetMapping("/public/products/search")
    public ResponseEntity<?> searchProducts(@RequestParam String name) {
        return ResponseEntity.ok(productService.searchProductsByName(name));
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping("/admin/products")
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (page >= 0 && size > 0) {
            Page<Product> products = productService.getAllProductsWithPagination(status, page, size);
            return ResponseEntity.ok(products);
        } else {
            List<Product> products = productService.getAllProducts(status);
            return ResponseEntity.ok(products);
        }
    }

    @GetMapping("/admin/products/search")
    public ResponseEntity<?> searchProductsWithFilters(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Product> products = productService.searchProducts(name, status, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/admin/products/{id}")
    public ResponseEntity<?> getProductByIdForAdmin(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping("/admin/products")
    public ResponseEntity<?> createProduct(@Valid @RequestBody UpsertProductRequest request) {
        return new ResponseEntity<>(productService.saveProduct(request), HttpStatus.CREATED);
    }

    @PutMapping("/admin/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @Valid @RequestBody UpsertProductRequest request) {
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/products/{id}/toggle-status")
    public ResponseEntity<?> toggleProductStatus(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.toggleProductStatus(id));
    }

    // ========== UTILITY ENDPOINTS ==========

    @GetMapping("/admin/products/check-sku")
    public ResponseEntity<?> checkSkuExists(@RequestParam String sku, @RequestParam(required = false) Integer excludeId) {
        boolean exists = excludeId != null 
            ? productService.existsBySkuAndNotId(sku, excludeId)
            : productService.existsBySku(sku);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @GetMapping("/admin/products/{id}/price")
    public ResponseEntity<?> getProductPrice(@PathVariable Integer id) {
        return pricingService.getPriceForProduct(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // ========== LEGACY ENDPOINTS (để backward compatibility) ==========
    
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProductsLegacy() {
        return ResponseEntity.ok(productService.getAllProducts(null));
    }
    
    @GetMapping("/products/status/{status}")
    public ResponseEntity<List<Product>> getProductsByStatus(@PathVariable Boolean status) {
        return ResponseEntity.ok(productService.getProductsByStatus(status));
    }
    
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProductByIdLegacy(@PathVariable Integer id) {
        Product product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }
    
    @GetMapping("/products/sku/{sku}")
    public ResponseEntity<Product> getProductBySkuLegacy(@PathVariable String sku) {
        Product product = productService.getProductBySku(sku);
        return ResponseEntity.ok(product);
    }
    
    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchProductsByNameLegacy(@RequestParam String name) {
        return ResponseEntity.ok(productService.searchProductsByName(name));
    }
    
    @PostMapping("/products")
    public ResponseEntity<Product> createProductLegacy(@Valid @RequestBody Product product) {
        Product createdProduct = productService.createProduct(product);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }
    
    @PutMapping("/products/{id}")
    public ResponseEntity<Product> updateProductLegacy(@PathVariable Integer id, 
                                               @Valid @RequestBody Product product) {
        Product updatedProduct = productService.updateProduct(id, product);
        return ResponseEntity.ok(updatedProduct);
    }
    
    @DeleteMapping("/products/{id}")
    public ResponseEntity<Void> deleteProductLegacy(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/products/{id}/toggle-status")
    public ResponseEntity<Product> toggleProductStatusLegacy(@PathVariable Integer id) {
        Product product = productService.toggleProductStatus(id);
        return ResponseEntity.ok(product);
    }
    
    @GetMapping("/products/{id}/price")
    public ResponseEntity<?> getProductPriceLegacy(@PathVariable Integer id) {
        return pricingService.getPriceForProduct(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}