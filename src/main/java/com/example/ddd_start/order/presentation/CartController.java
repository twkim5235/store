package com.example.ddd_start.order.presentation;

import com.example.ddd_start.order.application.model.CartDto;
import com.example.ddd_start.order.application.service.CartService;
import com.example.ddd_start.order.application.service.UpdateCartCommand;
import com.example.ddd_start.order.presentation.model.AddCartRequest;
import com.example.ddd_start.order.presentation.model.AddCartResponse;
import com.example.ddd_start.order.presentation.model.UpdateCartRequest;
import com.example.ddd_start.order.presentation.model.UpdateCartResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CartController {

  private final CartService cartService;

  @GetMapping("/carts")
  public ResponseEntity<List<CartDto>> printAllCarts(Authentication authentication) {
    List<CartDto> cartDtos = cartService.printAllCartsByUsername(authentication.getName());
    return ResponseEntity.ok().body(cartDtos);
  }

  @PostMapping("/carts")
  public ResponseEntity addCart(@RequestBody AddCartRequest req, Authentication authentication) {
    Long cartId = cartService.saveByUsername(
        authentication.getName(),
        req.productId(),
        req.quantity()
    );
    return ResponseEntity.ok(new AddCartResponse(cartId, "장바구니에 정상적으로 추가되었습니다."));
  }

  @PutMapping("/carts")
  public ResponseEntity updateCart(@RequestBody UpdateCartRequest req) {
    CartDto update = cartService.update(
        new UpdateCartCommand(req.cartId(), req.quantity())
    );

    return ResponseEntity
        .ok().body(new UpdateCartResponse(update, "장바구니 상품이 정상적으로 변경되었습니다."));
  }

  @DeleteMapping("/carts")
  public ResponseEntity deleteCart(@RequestParam Long cartId) {
    cartService.delete(cartId);

    String message = "정상적으로 삭제되었습니다.";
    return new ResponseEntity(message, HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/carts-all")
  public ResponseEntity deleteAllCarts(Authentication authentication) {
    cartService.deleteAllByUsername(authentication.getName());
    return new ResponseEntity("정상적으로 삭제되었습니다.", HttpStatus.ACCEPTED);
  }

}
