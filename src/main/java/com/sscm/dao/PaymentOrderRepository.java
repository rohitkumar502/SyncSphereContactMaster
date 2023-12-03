package com.sscm.dao;

import com.sscm.entities.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {

    public PaymentOrder findByOrderId(String orderId);

}
