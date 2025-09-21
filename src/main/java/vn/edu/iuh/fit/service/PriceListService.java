package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.PriceList;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.model.dto.PriceItemRequest;
import vn.edu.iuh.fit.model.enums.TargetType;
import vn.edu.iuh.fit.repository.PriceListRepository;
import vn.edu.iuh.fit.repository.PriceItemRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PriceListService {
    
    private final PriceListRepository priceListRepository;
    private final PriceItemRepository priceItemRepository;
    
    // Lấy tất cả bảng giá
    public List<PriceList> getAllPriceLists() {
        return priceListRepository.findAll();
    }
    
    // Lấy bảng giá theo trạng thái
    public List<PriceList> getPriceListsByStatus(Boolean status) {
        return priceListRepository.findByStatus(status);
    }
    
    // Lấy bảng giá theo ID
    public Optional<PriceList> getPriceListById(Integer id) {
        return priceListRepository.findById(id);
    }
    
    // Lấy bảng giá hiệu lực tại thời điểm hiện tại
    public List<PriceList> getValidPriceListsNow() {
        return priceListRepository.findValidPriceListsAt(new Date());
    }
    
    // Lấy bảng giá hiệu lực tại thời điểm cụ thể
    public List<PriceList> getValidPriceListsAt(Date checkDate) {
        return priceListRepository.findValidPriceListsAt(checkDate);
    }
    
    // Lấy bảng giá có priority cao nhất tại thời điểm hiện tại
    public PriceList getTopValidPriceListNow() {
        return priceListRepository.findTopValidPriceListAt(new Date());
    }
    
    // Tạo mới bảng giá
    public PriceList createPriceList(PriceList priceList) {
        return priceListRepository.save(priceList);
    }
    
    // Cập nhật bảng giá
    public PriceList updatePriceList(Integer id, PriceList priceListData) {
        PriceList existingPriceList = priceListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + id));
        
        existingPriceList.setName(priceListData.getName());
        existingPriceList.setStatus(priceListData.getStatus());
        existingPriceList.setPriority(priceListData.getPriority());
        existingPriceList.setValidFrom(priceListData.getValidFrom());
        existingPriceList.setValidTo(priceListData.getValidTo());
        
        return priceListRepository.save(existingPriceList);
    }
    
    // Xóa bảng giá
    public void deletePriceList(Integer id) {
        PriceList priceList = priceListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + id));
        
        priceListRepository.delete(priceList);
    }
    
    // Thay đổi trạng thái bảng giá
    public PriceList togglePriceListStatus(Integer id) {
        PriceList priceList = priceListRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + id));
        
        priceList.setStatus(!priceList.getStatus());
        return priceListRepository.save(priceList);
    }
    
    // Lấy các price item của bảng giá
    public List<PriceItem> getPriceItemsByPriceListId(Integer priceListId) {
        return priceItemRepository.findByPriceListId(priceListId);
    }
    
    // ============= PRICE ITEM METHODS =============
    
    // Lấy tất cả price items
    public List<PriceItem> getAllPriceItems() {
        return priceItemRepository.findAll();
    }
    
    // Lấy price item theo ID
    public Optional<PriceItem> getPriceItemById(Integer id) {
        return priceItemRepository.findById(id);
    }
    
    // Tạo mới price item
    public PriceItem createPriceItem(PriceItemRequest priceItemRequest) {
        // Get PriceList by ID
        PriceList priceList = priceListRepository.findById(priceItemRequest.getPriceListId())
                .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + priceItemRequest.getPriceListId()));
        
        // Convert DTO to Entity
        PriceItem priceItem = PriceItem.builder()
                .priceList(priceList)
                .targetType(priceItemRequest.getTargetType())
                .targetId(priceItemRequest.getTargetId())
                .seatType(priceItemRequest.getSeatType())
                .graphicsType(priceItemRequest.getGraphicsType())
                .screeningTimeType(priceItemRequest.getScreeningTimeType())
                .dayType(priceItemRequest.getDayType())
                .auditoriumType(priceItemRequest.getAuditoriumType())
                .price(priceItemRequest.getPrice())
                .minQty(priceItemRequest.getMinQty())
                .priority(priceItemRequest.getPriority())
                .status(priceItemRequest.getStatus() != null ? priceItemRequest.getStatus() : true)
                .build();
        
        return priceItemRepository.save(priceItem);
    }
    
    // Cập nhật price item
    public PriceItem updatePriceItem(Integer id, PriceItemRequest priceItemRequest) {
        PriceItem existingPriceItem = priceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceItem not found with id: " + id));
        
        // Get PriceList by ID if provided
        if (priceItemRequest.getPriceListId() != null) {
            PriceList priceList = priceListRepository.findById(priceItemRequest.getPriceListId())
                    .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + priceItemRequest.getPriceListId()));
            existingPriceItem.setPriceList(priceList);
        }
        
        existingPriceItem.setTargetType(priceItemRequest.getTargetType());
        existingPriceItem.setTargetId(priceItemRequest.getTargetId());
        existingPriceItem.setSeatType(priceItemRequest.getSeatType());
        existingPriceItem.setGraphicsType(priceItemRequest.getGraphicsType());
        existingPriceItem.setScreeningTimeType(priceItemRequest.getScreeningTimeType());
        existingPriceItem.setDayType(priceItemRequest.getDayType());
        existingPriceItem.setAuditoriumType(priceItemRequest.getAuditoriumType());
        existingPriceItem.setPrice(priceItemRequest.getPrice());
        existingPriceItem.setMinQty(priceItemRequest.getMinQty());
        existingPriceItem.setPriority(priceItemRequest.getPriority());
        
        if (priceItemRequest.getStatus() != null) {
            existingPriceItem.setStatus(priceItemRequest.getStatus());
        }
        
        return priceItemRepository.save(existingPriceItem);
    }
    
    // Xóa price item
    public void deletePriceItem(Integer id) {
        PriceItem priceItem = priceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceItem not found with id: " + id));
        
        priceItemRepository.delete(priceItem);
    }
    
    // Thay đổi trạng thái price item
    public PriceItem togglePriceItemStatus(Integer id) {
        PriceItem priceItem = priceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceItem not found with id: " + id));
        
        priceItem.setStatus(!priceItem.getStatus());
        return priceItemRepository.save(priceItem);
    }
    
    // Lấy price items theo target type
    public List<PriceItem> getPriceItemsByTargetType(TargetType targetType) {
        return priceItemRepository.findByTargetType(targetType);
    }
    
    // Lấy price items theo status
    public List<PriceItem> getPriceItemsByStatus(Boolean status) {
        return priceItemRepository.findByStatus(status);
    }
    
    // Lấy price items hiệu lực tại thời điểm hiện tại
    public List<PriceItem> getEffectivePriceItemsNow() {
        return priceItemRepository.findEffectivePriceItemsAt(new Date());
    }
    
    // Lấy price items hiệu lực tại thời điểm cụ thể
    public List<PriceItem> getEffectivePriceItemsAt(Date checkDate) {
        return priceItemRepository.findEffectivePriceItemsAt(checkDate);
    }
}