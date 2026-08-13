package vn.edu.iuh.fit.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.edu.iuh.fit.entity.Coupon;
import vn.edu.iuh.fit.repository.custom.CouponRepositoryCustom;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer>, CouponRepositoryCustom {

  boolean existsByCode(String code);

  Optional<Coupon> findByCode(String code);

  List<Coupon> findAll(Sort sort);

  @Query("SELECT c FROM Coupon c WHERE c.status = true")
  List<Coupon> findAllActive(Sort sort);

  @Query(
      "SELECT c FROM Coupon c "
          + "LEFT JOIN FETCH c.details d "
          + "LEFT JOIN FETCH d.terms "
          + "WHERE c.status = true")
  List<Coupon> findAllActiveWithDetailsAndTerms();
}
