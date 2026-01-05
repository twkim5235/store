package com.example.ddd_start.order.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderLineRepository extends JpaRepository<OrderLine, Long> {

  public void deleteByOrderId(Long orderId);
  public List<OrderLine> findByOrderId(Long orderId);

  @Query("select ol from OrderLine ol where ol.orderId in :orderIds")
  List<OrderLine> findByOrderIdIn(@Param("orderIds") List<Long> orderIds);
}
