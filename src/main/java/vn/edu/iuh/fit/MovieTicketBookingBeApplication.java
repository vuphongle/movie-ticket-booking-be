package vn.edu.iuh.fit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MovieTicketBookingBeApplication {

  public static void main(String[] args) {
    SpringApplication.run(MovieTicketBookingBeApplication.class, args);
  }
}
