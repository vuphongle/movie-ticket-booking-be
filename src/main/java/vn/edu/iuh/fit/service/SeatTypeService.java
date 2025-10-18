package vn.edu.iuh.fit.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import vn.edu.iuh.fit.model.enums.SeatType;

@Service
public class SeatTypeService {

  // Mapping between SeatType enum and integer IDs
  private static final Map<Integer, SeatType> ID_TO_SEAT_TYPE =
      Map.of(
          1, SeatType.NORMAL,
          2, SeatType.VIP,
          3, SeatType.COUPLE);

  private static final Map<SeatType, Integer> SEAT_TYPE_TO_ID =
      Map.of(
          SeatType.NORMAL, 1,
          SeatType.VIP, 2,
          SeatType.COUPLE, 3);

  /** Get SeatType by ID */
  public SeatType getSeatTypeById(Integer id) {
    return ID_TO_SEAT_TYPE.get(id);
  }

  /** Get ID by SeatType */
  public Integer getIdBySeatType(SeatType seatType) {
    return SEAT_TYPE_TO_ID.get(seatType);
  }

  /** Get all seat type options for frontend */
  public List<SeatTypeOption> getAllSeatTypeOptions() {
    return Arrays.stream(SeatType.values())
        .map(
            seatType ->
                new SeatTypeOption(
                    getIdBySeatType(seatType), seatType.name(), getSeatTypeDisplayName(seatType)))
        .collect(Collectors.toList());
  }

  /** Validate if seat type ID exists */
  public boolean isValidSeatTypeId(Integer id) {
    return ID_TO_SEAT_TYPE.containsKey(id);
  }

  /** Get display name for seat type */
  public String getSeatTypeDisplayName(SeatType seatType) {
    switch (seatType) {
      case NORMAL:
        return "Ghế thường";
      case VIP:
        return "Ghế VIP";
      case COUPLE:
        return "Ghế đôi";
      default:
        return seatType.name();
    }
  }

  /** DTO for frontend seat type options */
  public static class SeatTypeOption {
    private Integer id;
    private String key;
    private String label;

    public SeatTypeOption(Integer id, String key, String label) {
      this.id = id;
      this.key = key;
      this.label = label;
    }

    // Getters
    public Integer getId() {
      return id;
    }

    public String getKey() {
      return key;
    }

    public String getLabel() {
      return label;
    }

    // Setters
    public void setId(Integer id) {
      this.id = id;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public void setLabel(String label) {
      this.label = label;
    }
  }
}
