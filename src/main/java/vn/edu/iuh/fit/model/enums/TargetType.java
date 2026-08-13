package vn.edu.iuh.fit.model.enums;

public enum TargetType {
  PRODUCT, // Áp dụng cho sản phẩm
  ADDITIONAL_SERVICE, // Áp dụng cho dịch vụ bổ sung
  TICKET, // Áp dụng cho vé xem phim

  // Giữ lại để tương thích với code cũ
  @Deprecated
  ORDER, // Áp dụng cho toàn bộ đơn hàng
  @Deprecated
  SEAT_TYPE, // Áp dụng cho loại ghế cụ thể
  @Deprecated
  SERVICE // Áp dụng cho dịch vụ bổ sung
}
