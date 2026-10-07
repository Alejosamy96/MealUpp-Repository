package com.example.mealupp.repository;

import com.example.mealupp.model.Dish;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DishRepository extends JpaRepository<Dish, Long> {

  List<Dish> findByAvailableTrue();
}
