package vn.edu.iuh.fit.model.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenreDto {
    private Integer id;
    private String name;
    private String slug;
    private Date createdAt;
    private Date updatedAt;
}
