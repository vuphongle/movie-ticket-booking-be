package vn.edu.iuh.fit.constant;

public class ValidationMessages {
    
    // Header validation messages
    public static final String CODE_DUPLICATE = "Mã đã tồn tại, vui lòng chọn mã khác.";
    public static final String INVALID_TIME_RANGE = "Thời gian áp dụng không hợp lệ (ngày bắt đầu phải trước ngày kết thúc).";
    public static final String CANNOT_ACTIVATE_NO_ENABLED_DETAILS = "Không thể kích hoạt: cần ít nhất 1 dòng chi tiết bật 'enabled' và hợp lệ.";
    
    // Detail validation messages
    public static final String ORDER_NO_TARGET_REF = "ORDER không cho phép chọn đối tượng.";
    public static final String INVALID_PERCENT_VALUE = "Giá trị % không hợp lệ (0 < % ≤ 100).";
    public static final String INVALID_AMOUNT_VALUE = "Giá trị số tiền không hợp lệ (> 0).";
    public static final String INVALID_GIFT_CONFIG = "Cấu hình quà tặng không hợp lệ.";
    public static final String DETAIL_USAGE_EXCEEDED = "Ưu đãi đã hết lượt sử dụng.";
    public static final String CART_NOT_MEET_CONDITIONS = "Giỏ hàng chưa đáp ứng điều kiện của khuyến mãi.";
    public static final String DISCOUNT_EXCEEDS_ITEM_VALUE = "Giảm giá vượt quá giá trị món hàng.";
    
    // Apply validation messages
    public static final String COUPON_ALREADY_APPLIED = "Mã đã áp dụng cho đơn này.";
    public static final String COUPON_NOT_ACTIVE = "Mã khuyến mãi không khả dụng.";
    public static final String COUPON_NOT_STARTED = "Mã khuyến mãi chưa có hiệu lực.";
    public static final String COUPON_EXPIRED = "Mã khuyến mãi đã hết hạn.";
    public static final String NO_VALID_DETAILS = "Không có chi tiết nào phù hợp với điều kiện.";
    
    // Condition messages
    public static final String MIN_QUANTITY_NOT_MET = "Không đạt số lượng tối thiểu.";
    public static final String MIN_ORDER_TOTAL_NOT_MET = "Đơn hàng không đạt giá trị tối thiểu.";
    public static final String NO_MATCHING_ITEMS = "Không có sản phẩm phù hợp.";
    public static final String NO_SERVICES_IN_ORDER = "Không có dịch vụ trong đơn hàng.";
    
    // Delete/Update messages
    public static final String CANNOT_DELETE_USED_COUPON = "Coupon đã được sử dụng không thể xóa.";
    public static final String CANNOT_DELETE_USED_DETAIL = "Coupon detail đã được sử dụng không thể xóa.";
    public static final String CANNOT_REDUCE_USAGE_LIMIT = "Không thể đặt usage limit nhỏ hơn số lượng đã sử dụng.";
    
    // Success messages
    public static final String COUPON_APPLIED_SUCCESSFULLY = "Áp dụng thành công cho toàn bộ đơn hàng.";
    public static final String SEAT_TYPE_APPLIED_SUCCESSFULLY = "Áp dụng thành công cho loại ghế.";
    public static final String SERVICE_APPLIED_SUCCESSFULLY = "Áp dụng thành công cho dịch vụ.";
    public static final String ADD_DETAIL_REMINDER = "Hãy thêm ít nhất 1 dòng chi tiết.";
}