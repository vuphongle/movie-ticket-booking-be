package vn.edu.iuh.fit.model.enums;

public enum CouponType {
    PROMOTION,      // Promotion - hiện cho user chọn (code = null)
    VOUCHER         // Voucher - user nhập mã (code = not null & unique)
}