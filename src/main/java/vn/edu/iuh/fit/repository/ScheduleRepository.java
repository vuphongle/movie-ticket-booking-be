package vn.edu.iuh.fit.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.iuh.fit.entity.Schedule;

import java.util.Date;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByMovie_StatusAndStartDateBeforeAndEndDateAfter(Boolean movieStatus, Date startDate, Date endDate);

    List<Schedule> findByMovie_StatusAndStartDateAfter(Boolean movieStatus, Date date);
}
