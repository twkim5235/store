package com.example.ddd_start.order.presentation;

import com.example.ddd_start.common.domain.exception.NoMemberFoundException;
import com.example.ddd_start.common.domain.exception.ValidationErrorException;
import com.example.ddd_start.coupon.Exception.CouponAlreadyUsedException;
import com.example.ddd_start.order.application.model.ChangeOrderShippingInfoCommand;
import com.example.ddd_start.order.application.model.FindOrderResponse;
import com.example.ddd_start.order.application.model.PlaceOrderCommand;
import com.example.ddd_start.order.application.model.UpdateOrderCommand;
import com.example.ddd_start.order.application.service.OrderService;
import com.example.ddd_start.order.presentation.model.PlaceOrderRequest;
import com.example.ddd_start.order.presentation.model.PlaceOrderResponse;
import com.example.ddd_start.order.presentation.model.UpdateOrderRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

  private final OrderService orderService;

  @GetMapping("/orders/my-order")
  public ResponseEntity findMyOrder(Authentication authentication) {
    try {
      List<FindOrderResponse> myOrder = orderService.findMyOrderByUsername(authentication.getName());
      return ResponseEntity.ok(myOrder);
    } catch (NoMemberFoundException e) {
      return ResponseEntity.badRequest().body("회원 정보를 찾을 수 없습니다.");
    }
  }

  @PostMapping("/orders/place-order")
  public ResponseEntity order(@RequestBody PlaceOrderRequest req, BindingResult bindingResult) {
    try {
      Long orderId = orderService.placeOrderV2(
          new PlaceOrderCommand(
              req.orderLines(),
              req.shippingInfo(),
              req.message(),
              req.orderer(),
              req.paymentInfo(),
              req.coupons()
          )
      );
      return ResponseEntity
          .ok(new PlaceOrderResponse("주문이 정상적으로 완료되었습니다.", orderId));
    } catch (ValidationErrorException e) {
      e.getErrors().forEach(err -> {
        if (err.hasName()) {
          bindingResult.rejectValue(err.getPropertyName(), err.getValue());
        } else {
          bindingResult.reject(err.getValue());
        }
      });

      throw new RuntimeException(e);
    } catch (NoMemberFoundException e) {
      return ResponseEntity.badRequest().body("회원 정보를 찾을 수 없습니다.");
    } catch (CouponAlreadyUsedException e) {
      return ResponseEntity.badRequest().body("이미 사용한 쿠폰입니다.");
    }
  }

  @PostMapping("/orders/shipping-info")
  public ResponseEntity changeShippingInfo(ChangeOrderShippingInfoCommand command) {
    try {
      orderService.changeShippingInfo(command);
      return ResponseEntity.ok("배송 정보가 정상적으로 변경되었습니다.");
    } catch (NoMemberFoundException e) {
      return ResponseEntity.badRequest().body("회원 정보를 찾을 수 없습니다.");
    }
  }

  @PutMapping("/orders")
  public ResponseEntity update(@RequestBody UpdateOrderRequest req, BindingResult bindingResult) {

    try {
      Long orderId = orderService.updateOrder(
          new UpdateOrderCommand(
              req.orderId(),
              req.shippingInfo()
          )
      );
      return ResponseEntity
          .ok(new PlaceOrderResponse("배달지가 정상적으로 변경되었습니다.", orderId));
    } catch (IllegalStateException e) {
      return ResponseEntity.badRequest().body(e.getMessage());
    }

  }

  @DeleteMapping("/orders")
  public ResponseEntity cancelOrder(@RequestParam Long orderId) {
    orderService.cancelOrder(orderId);
    return ResponseEntity.ok(
        "주문이 정상적으로 취소되었습니다."
    );
  }
}
