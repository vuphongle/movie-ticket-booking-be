package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.PriceList;
import vn.edu.iuh.fit.entity.PriceItem;
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
}