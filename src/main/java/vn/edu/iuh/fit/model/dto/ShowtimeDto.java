package vn.edu.iuh.fit.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ShowtimeDto {
    private Integer id;
    private LocalDate date;
    private String startTime;
    private String endTime;
    private String graphicsType;
    private String translationType;
    private Integer cinemaId;
    private String cinemaName;
    private String location;
    private Integer auditoriumId;
    private String auditoriumName;
    private Integer totalSeats;
    private Integer totalRows;
    private Integer totalColumns;
    private String auditoriumType;
    private Date createdAt;
    private Date updatedAt;
}