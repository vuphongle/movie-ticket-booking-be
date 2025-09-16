package vn.edu.iuh.fit.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import vn.edu.iuh.fit.model.enums.AuditoriumType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpsertAuditorium {
    @NotEmpty(message = "Tên không được để trống")
    String name;

    @NotNull(message = "Số hàng không được để trống")
    Integer totalRows;

    @NotNull(message = "Số cột không được để trống")
    Integer totalColumns;

    @NotNull(message = "Loại phòng chiếu không được để trống")
    AuditoriumType type;

    @NotNull(message = "ID rạp chiếu không được để trống")
    Integer cinemaId;
}
