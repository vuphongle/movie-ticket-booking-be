package vn.edu.iuh.fit.model.enums;

/**
 * Enum representing the kind of coupon - DISPLAY: Coupon for marketing display purposes, code can
 * be null - VOUCHER: Coupon that requires a unique code for redemption
 */
public enum CouponKind {
  /**
   * Display coupon for marketing purposes Does not require a code, used for promotional displays
   */
  DISPLAY,

  /** Voucher coupon that requires a unique code Must have a code for users to redeem */
  VOUCHER
}
