package com.restaurant.repository;

import com.restaurant.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    @Query("""
            select i from MenuItem i
            join fetch i.category
            where i.active = true and i.popular = true
            order by i.name
            """)
    List<MenuItem> findFeatured();

    @Query("""
            select i from MenuItem i
            join fetch i.category
            order by i.category.displayOrder, i.name
            """)
    List<MenuItem> findAllWithCategory();
}
