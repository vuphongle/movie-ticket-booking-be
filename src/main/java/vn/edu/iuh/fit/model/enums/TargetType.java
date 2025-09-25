package vn.edu.iuh.fit.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TargetType {
    PRODUCT("Sản phẩm"),
    ADDITIONAL_SERVICE("Dịch vụ thêm"),
    TICKET("Vé xem phim");

    private final String description;
}