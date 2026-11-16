package com.example.mealupp.repository;

import com.example.mealupp.model.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

  Optional<Payment> findByProviderReference(String providerReference);

  List<Payment> findByCustomerOrderId(Long orderId);
}
