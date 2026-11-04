package com.restaurant.repository;

import com.restaurant.entity.MenuCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    List<MenuCategory> findByActiveTrueOrderByDisplayOrderAsc();

    List<MenuCategory> findAllByOrderByDisplayOrderAsc();

    Optional<MenuCategory> findByNameIgnoreCase(String name);
}
