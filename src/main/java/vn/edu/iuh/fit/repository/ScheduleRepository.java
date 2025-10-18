package vn.edu.iuh.fit.repository;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Schedule;

public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
  List<Schedule> findByMovie_StatusAndStartDateBeforeAndEndDateAfter(
      Boolean movieStatus, Date startDate, Date endDate);

  List<Schedule> findByMovie_StatusAndStartDateAfter(Boolean movieStatus, Date date);

  List<Schedule> findByMovie_StatusAndEndDateAfter(Boolean movieStatus, Date date);

  List<Schedule> findByMovie_Id(Integer movieId);

  List<Schedule> findByMovieIdAndEndDateAfter(Integer movieId, Date startDate);
}
