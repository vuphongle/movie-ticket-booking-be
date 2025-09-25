package vn.edu.iuh.fit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.iuh.fit.entity.CouponDetail;
import vn.edu.iuh.fit.exception.BadRequestException;
import vn.edu.iuh.fit.exception.ResourceNotFoundException;
import vn.edu.iuh.fit.repository.CouponDetailRepository;
import vn.edu.iuh.fit.repository.CouponRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CouponDetailService {
    private final CouponDetailRepository couponDetailRepository;
    private final CouponRepository couponRepository;

    public List<CouponDetail> getCouponDetailsByCouponId(Integer couponId) {
        log.info("Getting coupon details for couponId: {}", couponId);
        
        // Validate coupon exists
        if (!couponRepository.existsById(couponId)) {
            throw new ResourceNotFoundException("Coupon not found with id: " + couponId);
        }
        
        return couponDetailRepository.findByCouponId(couponId);
    }

    public CouponDetail getCouponDetailById(Integer id) {
        log.info("Getting coupon detail with id: {}", id);
        
        return couponDetailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon detail not found with id: " + id));
    }

    public CouponDetail createCouponDetail(CouponDetail couponDetail) {
        log.info("Creating coupon detail for couponId: {}", couponDetail.getCouponId());
        
        validateCouponDetail(couponDetail);
        
        couponDetail.setCreatedAt(LocalDateTime.now());
        couponDetail.setUpdatedAt(LocalDateTime.now());
        
        return couponDetailRepository.save(couponDetail);
    }

    public CouponDetail updateCouponDetail(Integer id, CouponDetail updatedDetail) {
        log.info("Updating coupon detail with id: {}", id);
        
        CouponDetail existingDetail = getCouponDetailById(id);
        
        // Update fields
        existingDetail.setCouponId(updatedDetail.getCouponId());
        existingDetail.setTargetType(updatedDetail.getTargetType());
        existingDetail.setGiftServiceId(updatedDetail.getGiftServiceId());
        existingDetail.setUpdatedAt(LocalDateTime.now());
        
        validateCouponDetail(existingDetail);
        
        return couponDetailRepository.save(existingDetail);
    }

    public void deleteCouponDetail(Integer id) {
        log.info("Deleting coupon detail with id: {}", id);
        
        if (!couponDetailRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon detail not found with id: " + id);
        }
        
        couponDetailRepository.deleteById(id);
    }

    // VALIDATION methods
    private void validateCouponDetail(CouponDetail couponDetail) {
        // Validate coupon exists
        if (!couponRepository.existsById(couponDetail.getCouponId())) {
            throw new BadRequestException("Coupon not found with id: " + couponDetail.getCouponId());
        }
        
        // Validate no duplicate giftServiceId for same coupon
        if (couponDetail.getGiftServiceId() != null) {
            // Check if giftServiceId already exists for this coupon
            List<CouponDetail> existingDetails = couponDetailRepository.findByCouponId(couponDetail.getCouponId());
            boolean exists = existingDetails.stream()
                    .anyMatch(detail -> couponDetail.getGiftServiceId().equals(detail.getGiftServiceId()) 
                            && !detail.getId().equals(couponDetail.getId()));
            if (exists) {
                throw new BadRequestException("Gift service already exists for this coupon");
            }
        }
    }

    public CouponDetail duplicateCouponDetail(Integer id) {
        log.info("Duplicating coupon detail with id: {}", id);
        
        CouponDetail original = getCouponDetailById(id);
        
        CouponDetail duplicate = CouponDetail.builder()
                .couponId(original.getCouponId())
                .targetType(original.getTargetType())
                .giftServiceId(original.getGiftServiceId())
                .build();
        
        return createCouponDetail(duplicate);
    }
}
