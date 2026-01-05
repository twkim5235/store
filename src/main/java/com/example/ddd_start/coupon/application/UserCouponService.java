package com.example.ddd_start.coupon.application;

import com.example.ddd_start.coupon.application.model.UserCouponDto;
import com.example.ddd_start.coupon.domain.CouponDefinition;
import com.example.ddd_start.coupon.domain.CouponDefinitionRepository;
import com.example.ddd_start.coupon.domain.UserCoupon;
import com.example.ddd_start.coupon.domain.UserCouponRepository;
import com.example.ddd_start.member.domain.Member;
import com.example.ddd_start.member.domain.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCouponService {

  private final UserCouponRepository userCouponRepository;
  private final CouponDefinitionRepository couponDefinitionRepository;
  private final MemberRepository memberRepository;

  @Transactional(readOnly = true)
  public List<UserCouponDto> findByMemberId(Long memberId) {
    return userCouponRepository.findAllByMemberIdAndIsUsedFalse(memberId)
        .stream()
        .map(userCoupon -> new UserCouponDto(
                userCoupon.getId(),
                userCoupon.getName(),
                userCoupon.getIsUsed(),
                userCoupon.getIsRatio(),
                userCoupon.getRatio(),
                userCoupon.getFixedAmount()
            )
        )
        .toList();
  }

  @Transactional
  public Long register(Long memberId, Long couponDefinitionId) {
    CouponDefinition couponDefinition = couponDefinitionRepository.findById(couponDefinitionId)
        .orElseThrow(() -> new RuntimeException("존재하지 않는 쿠폰입니다."));
    Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new RuntimeException("존재하지 않는 회원입니다."));

    UserCoupon userCoupon = userCouponRepository.save(
        new UserCoupon(member, couponDefinition));
    return userCoupon.getId();
  }

  @Transactional
  public void update(UserCouponDto userCouponDto) {
    UserCoupon userCoupon = userCouponRepository.findById(userCouponDto.id())
        .orElseThrow(() -> new RuntimeException("해당 쿠폰이 존재하지 않습니다."));

    userCoupon.update(
        userCouponDto.name(),
        userCouponDto.isUsed(),
        userCouponDto.isRatio(),
        userCouponDto.ratio(),
        userCouponDto.fixedAmount()
    );
  }

  @Transactional
  public void delete(Long userCouponId) {
    userCouponRepository.deleteById(userCouponId);
  }
}
