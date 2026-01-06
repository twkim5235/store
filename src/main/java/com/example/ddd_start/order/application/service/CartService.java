package com.example.ddd_start.order.application.service;

import com.example.ddd_start.member.domain.Member;
import com.example.ddd_start.member.domain.MemberRepository;
import com.example.ddd_start.order.application.model.CartDto;
import com.example.ddd_start.order.application.model.CreateCartCommand;
import com.example.ddd_start.order.domain.Cart;
import com.example.ddd_start.order.domain.CartRepository;
import com.example.ddd_start.product.domain.Product;
import com.example.ddd_start.product.domain.ProductRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CartService {

  private final CartRepository cartRepository;
  private final MemberRepository memberRepository;
  private final ProductRepository productRepository;

  @Transactional
  public Long save(CreateCartCommand cmd) {
    Member member = memberRepository.findById(cmd.memberId())
        .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    Product product = productRepository.findById(cmd.productId())
        .orElseThrow(() -> new IllegalArgumentException("Product not found"));

    Optional<Cart> byMemberIdAndProductId = cartRepository.findByMemberIdAndProductId(
        member.getId(), product.getId());

    if(byMemberIdAndProductId.isPresent()) {
      Cart cart = byMemberIdAndProductId.get();
      cart.add(cmd.quantity());
      cartRepository.save(cart);
      return cart.getId();
    }

    Cart cart = new Cart(member,
        product,
        cmd.quantity());

    Cart save = cartRepository.save(cart);
    return save.getId();
  }

  @Transactional
  public Long saveByUsername(String username, Long productId, Integer quantity) {
    Member member = memberRepository.findMemberByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    return save(new CreateCartCommand(member.getId(), productId, quantity));
  }

  @Transactional(readOnly = true)
  public List<CartDto> printAllCarts(Long memberId) {
    return cartRepository.findByMemberId(memberId).stream()
        .map(cart -> new CartDto(
            cart.getId(),
            cart.getProduct().getId(),
            cart.getProduct().getTitle(),
            cart.getProduct().getPrice(),
            cart.getProduct().getImages().get(0),
            cart.getQuantity()
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<CartDto> printAllCartsByUsername(String username) {
    Member member = memberRepository.findMemberByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    return printAllCarts(member.getId());
  }

  @Transactional
  public CartDto update(UpdateCartCommand cmd) {
    Cart cart = cartRepository.findById(cmd.cartId())
        .orElseThrow(() -> new IllegalArgumentException("Cart not found"));

    cart.update(cmd.quantity());

    cartRepository.save(cart);
    return new CartDto(
        cart.getId(),
        cart.getProduct().getId(),
        cart.getProduct().getTitle(),
        cart.getProduct().getPrice(),
        cart.getProduct().getImages().get(0),
        cart.getQuantity()
    );
  }

  @Transactional
  public void delete(Long cartId) {
    cartRepository.deleteById(cartId);
  }

  @Transactional
  public void deleteAll(Long memberId) {
    cartRepository.deleteByMemberId(memberId);
  }

  @Transactional
  public void deleteAllByUsername(String username) {
    Member member = memberRepository.findMemberByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    deleteAll(member.getId());
  }

}
