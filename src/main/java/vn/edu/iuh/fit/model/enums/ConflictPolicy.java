package vn.edu.iuh.fit.model.enums;

public enum ConflictPolicy {
  FAIL, // Dừng khi có conflict, trả về lỗi
  SKIP // Bỏ qua ngày có conflict, tạo các ngày còn lại
}
