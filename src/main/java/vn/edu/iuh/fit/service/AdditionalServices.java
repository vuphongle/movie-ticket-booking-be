package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.AdditionalService;
import vn.edu.iuh.fit.entity.AdditionalServiceItem;
import vn.edu.iuh.fit.entity.Product;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.model.enums.AdditionalServiceType;
import vn.edu.iuh.fit.model.request.UpsertAdditionalServiceRequest;
import vn.edu.iuh.fit.repository.AdditionalServiceRepository;
import vn.edu.iuh.fit.repository.AdditionalServiceItemRepository;
import vn.edu.iuh.fit.repository.ProductRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdditionalServices {
    private final AdditionalServiceRepository additionalServiceRepository;
    private final AdditionalServiceItemRepository additionalServiceItemRepository;
    private final ProductRepository productRepository;
    private final PricingService pricingService;

    public List<AdditionalService> getAllAdditionalServicesByStatus(Boolean status) {
        return additionalServiceRepository.findByStatus(status);
    }

    public List<AdditionalService> getAllAdditionalServices() {
        return additionalServiceRepository.findAll();
    }

    public AdditionalService getAdditionalServiceById(Integer id) {
        return additionalServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dịch vụ có id = " + id));
    }

    public AdditionalService saveAdditionalService(UpsertAdditionalServiceRequest request) {
        // Validate request
        validateAdditionalServiceRequest(request);
        
        AdditionalService additionalService = AdditionalService.builder()
                .name(request.getName())
                .description(request.getDescription())
                .thumbnail(request.getThumbnail())
                .type(request.getType())
                .productId(request.getProductId())
                .defaultQuantity(request.getDefaultQuantity())
                .status(request.getStatus())
                .build();

        additionalService = additionalServiceRepository.save(additionalService);
        
        // Nếu là COMBO, tạo các AdditionalServiceItem
        if (request.getType() == AdditionalServiceType.COMBO && request.getItems() != null) {
            createAdditionalServiceItems(additionalService, request.getItems());
        }

        return additionalService;
    }

    public AdditionalService updateAdditionalService(Integer id, UpsertAdditionalServiceRequest request) {
        AdditionalService additionalService = getAdditionalServiceById(id);
        
        // Validate request
        validateAdditionalServiceRequest(request);
        
        additionalService.setName(request.getName());
        additionalService.setDescription(request.getDescription());
        additionalService.setThumbnail(request.getThumbnail());
        additionalService.setType(request.getType());
        additionalService.setProductId(request.getProductId());
        additionalService.setDefaultQuantity(request.getDefaultQuantity());
        additionalService.setStatus(request.getStatus());

        additionalService = additionalServiceRepository.save(additionalService);
        
        // Cập nhật items cho COMBO
        if (request.getType() == AdditionalServiceType.COMBO) {
            // Xóa items cũ
            additionalServiceItemRepository.deleteByAdditionalServiceId(id);
            
            // Tạo items mới
            if (request.getItems() != null) {
                createAdditionalServiceItems(additionalService, request.getItems());
            }
        }

        return additionalService;
    }

    public void deleteAdditionalService(Integer id) {
        AdditionalService additionalService = getAdditionalServiceById(id);
        additionalServiceRepository.delete(additionalService);
    }
    
    // Lấy giá cho additional service
    public Integer getPriceForAdditionalService(Integer serviceId) {
        return pricingService.getPriceForAdditionalService(serviceId)
                .orElse(null); // Trả về null nếu không tìm thấy giá
    }
    
    // Lấy các items của combo service
    public List<AdditionalServiceItem> getAdditionalServiceItems(Integer serviceId) {
        return additionalServiceItemRepository.findByAdditionalServiceIdWithProduct(serviceId);
    }
    
    private void validateAdditionalServiceRequest(UpsertAdditionalServiceRequest request) {
        if (request.getType() == AdditionalServiceType.SINGLE) {
            if (request.getProductId() == null) {
                throw new IllegalArgumentException("Product ID is required for SINGLE type service");
            }
            
            // Kiểm tra product có tồn tại không
            productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));
                    
            if (request.getDefaultQuantity() == null || request.getDefaultQuantity() <= 0) {
                throw new IllegalArgumentException("Default quantity must be greater than 0 for SINGLE type service");
            }
        } else if (request.getType() == AdditionalServiceType.COMBO) {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new IllegalArgumentException("Items are required for COMBO type service");
            }
        }
    }
    
    private void createAdditionalServiceItems(AdditionalService additionalService, 
                                            List<UpsertAdditionalServiceRequest.AdditionalServiceItemRequest> itemRequests) {
        for (UpsertAdditionalServiceRequest.AdditionalServiceItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));
            
            if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            
            AdditionalServiceItem item = AdditionalServiceItem.builder()
                    .additionalService(additionalService)
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .build();
                    
            additionalServiceItemRepository.save(item);
        }
    }
}
