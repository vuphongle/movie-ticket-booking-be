package vn.edu.iuh.fit.service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.OrderServiceItem;
import vn.edu.iuh.fit.entity.OrderTicketItem;
import vn.edu.iuh.fit.entity.PriceItem;
import vn.edu.iuh.fit.entity.PriceList;
import vn.edu.iuh.fit.model.dto.ClonePriceListRequest;
import vn.edu.iuh.fit.model.dto.PriceItemRequest;
import vn.edu.iuh.fit.model.enums.TargetType;
import vn.edu.iuh.fit.repository.OrderServiceItemRepository;
import vn.edu.iuh.fit.repository.OrderTicketItemRepository;
import vn.edu.iuh.fit.repository.PriceItemRepository;
import vn.edu.iuh.fit.repository.PriceListRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class PriceListService {

  private final PriceListRepository priceListRepository;
  private final PriceItemRepository priceItemRepository;
  private final OrderServiceItemRepository orderServiceItemRepository;
  private final OrderTicketItemRepository orderTicketItemRepository;

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
    PriceList existingPriceList =
        priceListRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + id));

    existingPriceList.setName(priceListData.getName());
    existingPriceList.setStatus(priceListData.getStatus());
    existingPriceList.setValidFrom(priceListData.getValidFrom());
    existingPriceList.setValidTo(priceListData.getValidTo());

    return priceListRepository.save(existingPriceList);
  }

  // Xóa bảng giá
  public void deletePriceList(Integer id) {
    PriceList priceList =
        priceListRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + id));

    priceListRepository.delete(priceList);
  }

  // Thay đổi trạng thái bảng giá
  public PriceList togglePriceListStatus(Integer id) {
    PriceList priceList =
        priceListRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + id));

    priceList.setStatus(!priceList.getStatus());
    return priceListRepository.save(priceList);
  }

  // Nhân bản bảng giá cùng với tất cả price items
  public PriceList clonePriceList(Integer sourceId, ClonePriceListRequest request) {
    // Tìm bảng giá gốc
    PriceList sourcePriceList =
        priceListRepository
            .findById(sourceId)
            .orElseThrow(() -> new RuntimeException("PriceList not found with id: " + sourceId));

    // Tạo bảng giá mới
    PriceList clonedPriceList =
        PriceList.builder()
            .name(request.getName())
            .status(
                request.getStatus() != null ? request.getStatus() : false) // Default to inactive
            .validFrom(
                request.getValidFrom() != null
                    ? request.getValidFrom()
                    : sourcePriceList.getValidFrom())
            .validTo(
                request.getValidTo() != null ? request.getValidTo() : sourcePriceList.getValidTo())
            .build();

    // Lưu bảng giá mới
    PriceList savedPriceList = priceListRepository.save(clonedPriceList);

    // Lấy tất cả price items của bảng giá gốc
    List<PriceItem> sourcePriceItems = priceItemRepository.findByPriceListId(sourceId);

    // Clone từng price item
    for (PriceItem sourceItem : sourcePriceItems) {
      PriceItem clonedItem =
          PriceItem.builder()
              .priceList(savedPriceList)
              .targetType(sourceItem.getTargetType())
              .targetId(sourceItem.getTargetId())
              .seatType(sourceItem.getSeatType())
              .graphicsType(sourceItem.getGraphicsType())
              .screeningTimeType(sourceItem.getScreeningTimeType())
              .dayType(sourceItem.getDayType())
              .auditoriumType(sourceItem.getAuditoriumType())
              .price(sourceItem.getPrice())
              .minQty(sourceItem.getMinQty())
              .status(sourceItem.getStatus()) // Giữ nguyên status của từng item
              .build();

      priceItemRepository.save(clonedItem);
    }

    return savedPriceList;
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

    // Tạo mới PriceItem
    public PriceItem createPriceItem(PriceItemRequest priceItemRequest) {
        // Lấy PriceList theo ID
        PriceList priceList = priceListRepository.findById(priceItemRequest.getPriceListId())
                .orElseThrow(() ->
                        new RuntimeException("PriceList not found with id: " + priceItemRequest.getPriceListId()));

        List<PriceItem> existingItems = priceItemRepository.findByPriceListId(priceItemRequest.getPriceListId());

        for (PriceItem item : existingItems) {
            // Bỏ qua item không hoạt động
//            if (Boolean.FALSE.equals(item.getStatus())) continue;

            // So sánh loại target và targetId
            if (item.getTargetType() == priceItemRequest.getTargetType() &&
                    Objects.equals(item.getTargetId(), priceItemRequest.getTargetId())) {

                switch (item.getTargetType()) {
                    case TICKET -> {
                        // Kiểm tra các điều kiện vé
                        boolean sameCondition =
                                Objects.equals(item.getSeatType(), priceItemRequest.getSeatType()) &&
                                        Objects.equals(item.getGraphicsType(), priceItemRequest.getGraphicsType()) &&
                                        Objects.equals(item.getScreeningTimeType(), priceItemRequest.getScreeningTimeType()) &&
                                        Objects.equals(item.getDayType(), priceItemRequest.getDayType()) &&
                                        Objects.equals(item.getAuditoriumType(), priceItemRequest.getAuditoriumType());

                        if (sameCondition) {
                            throw new RuntimeException(
                                    "A TICKET PriceItem with the same conditions already exists and is active in this PriceList."
                            );
                        }
                    }
                    case PRODUCT, ADDITIONAL_SERVICE -> {
                        // Nếu có item đang hoạt động cùng targetType + targetId
                        throw new RuntimeException(
                                "A " + item.getTargetType() + " PriceItem with the same target already exists and is active in this PriceList."
                        );
                    }
                    default -> {
                        // Phòng trường hợp targetType khác (nếu thêm về sau)
                        throw new RuntimeException("Unsupported target type: " + item.getTargetType());
                    }
                }
            }
        }

        // Nếu không trùng, tạo mới PriceItem
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
                .status(priceItemRequest.getStatus() != null ? priceItemRequest.getStatus() : true)
                .build();

        return priceItemRepository.save(priceItem);
    }


    // Cập nhật PriceItem
    public PriceItem updatePriceItem(Integer id, PriceItemRequest priceItemRequest) {
        PriceItem existingPriceItem = priceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PriceItem not found with id: " + id));

        // Kiểm tra giá đã được sử dụng trong đơn hàng chưa
        OrderServiceItem usedService = orderServiceItemRepository.findByPriceItemId(id);
        OrderTicketItem usedTicket = orderTicketItemRepository.findByPriceItemId(id);

        if (usedService != null || usedTicket != null) {
            throw new RuntimeException("Cannot edit PriceItem that has been used in orders.");
        }

        // Lấy PriceList hiện tại hoặc theo ID truyền vào
        PriceList priceList = existingPriceItem.getPriceList();
        if (priceItemRequest.getPriceListId() != null) {
            priceList = priceListRepository.findById(priceItemRequest.getPriceListId())
                    .orElseThrow(() -> new RuntimeException(
                            "PriceList not found with id: " + priceItemRequest.getPriceListId()));
            existingPriceItem.setPriceList(priceList);
        }

        // Kiểm tra trùng với item khác trong cùng PriceList
        List<PriceItem> existingItems = priceItemRepository.findByPriceListId(priceList.getId());
        for (PriceItem item : existingItems) {
            // Bỏ qua chính item đang sửa hoặc các item không hoạt động
            if (item.getId().equals(id)) continue;
//            if (Boolean.FALSE.equals(item.getStatus())) continue;

            // Kiểm tra cùng targetType và targetId
            if (item.getTargetType() == priceItemRequest.getTargetType() &&
                    Objects.equals(item.getTargetId(), priceItemRequest.getTargetId())) {

                switch (item.getTargetType()) {
                    case TICKET -> {
                        boolean sameCondition =
                                Objects.equals(item.getSeatType(), priceItemRequest.getSeatType()) &&
                                        Objects.equals(item.getGraphicsType(), priceItemRequest.getGraphicsType()) &&
                                        Objects.equals(item.getScreeningTimeType(), priceItemRequest.getScreeningTimeType()) &&
                                        Objects.equals(item.getDayType(), priceItemRequest.getDayType()) &&
                                        Objects.equals(item.getAuditoriumType(), priceItemRequest.getAuditoriumType());

                        if (sameCondition) {
                            throw new RuntimeException(
                                    "Another active TICKET PriceItem with the same conditions already exists in this PriceList."
                            );
                        }
                    }
                    case PRODUCT, ADDITIONAL_SERVICE -> {
                        throw new RuntimeException(
                                "Another active " + item.getTargetType() + " PriceItem with the same target already exists in this PriceList."
                        );
                    }
                    default -> {
                        throw new RuntimeException("Unsupported target type: " + item.getTargetType());
                    }
                }
            }
        }

        // Cập nhật thông tin
        existingPriceItem.setTargetType(priceItemRequest.getTargetType());
        existingPriceItem.setTargetId(priceItemRequest.getTargetId());
        existingPriceItem.setSeatType(priceItemRequest.getSeatType());
        existingPriceItem.setGraphicsType(priceItemRequest.getGraphicsType());
        existingPriceItem.setScreeningTimeType(priceItemRequest.getScreeningTimeType());
        existingPriceItem.setDayType(priceItemRequest.getDayType());
        existingPriceItem.setAuditoriumType(priceItemRequest.getAuditoriumType());
        existingPriceItem.setPrice(priceItemRequest.getPrice());
        existingPriceItem.setMinQty(priceItemRequest.getMinQty());

        if (priceItemRequest.getStatus() != null) {
            existingPriceItem.setStatus(priceItemRequest.getStatus());
        }

        return priceItemRepository.save(existingPriceItem);
    }


    // Xóa price item
  public void deletePriceItem(Integer id) {
      // Kiểm tra giá đã được sử dụng trong đơn hàng chưa
      OrderServiceItem usedService = orderServiceItemRepository.findByPriceItemId(id);
      OrderTicketItem usedTicket = orderTicketItemRepository.findByPriceItemId(id);

      if (usedService != null || usedTicket != null) {
          throw new RuntimeException("Cannot delete PriceItem that has been used in orders.");
      }
    PriceItem priceItem =
        priceItemRepository
            .findById(id)
            .orElseThrow(() -> new RuntimeException("PriceItem not found with id: " + id));

    priceItemRepository.delete(priceItem);
  }

  // Thay đổi trạng thái price item
  public PriceItem togglePriceItemStatus(Integer id) {
    PriceItem priceItem =
        priceItemRepository
            .findById(id)
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
