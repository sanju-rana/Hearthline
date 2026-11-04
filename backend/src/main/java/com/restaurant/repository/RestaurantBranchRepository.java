package com.restaurant.repository;

import com.restaurant.entity.RestaurantBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RestaurantBranchRepository extends JpaRepository<RestaurantBranch, Long> {

    @Query("""
            select b from RestaurantBranch b
            join fetch b.restaurant
            where b.active = true
            order by b.city, b.name
            """)
    List<RestaurantBranch> findActiveWithRestaurant();

    @Query("""
            select b from RestaurantBranch b
            join fetch b.restaurant
            order by b.city, b.name
            """)
    List<RestaurantBranch> findAllWithRestaurant();

    long countByActiveTrue();
}
