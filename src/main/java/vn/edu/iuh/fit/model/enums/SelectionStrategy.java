package vn.edu.iuh.fit.model.enums;

public enum SelectionStrategy {
  HIGHEST_PRICE_FIRST, // Chọn item có giá cao nhất trước (mặc định)
  LOWEST_PRICE_FIRST, // Chọn item có giá thấp nhất trước
  FIFO // First In First Out - theo thứ tự trong giỏ hàng
}
