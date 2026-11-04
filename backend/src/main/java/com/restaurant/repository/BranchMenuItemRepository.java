package com.restaurant.repository;

import com.restaurant.entity.BranchMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BranchMenuItemRepository extends JpaRepository<BranchMenuItem, Long> {

    @Query("""
            select bmi from BranchMenuItem bmi
            join fetch bmi.menuItem item
            join fetch item.category
            where bmi.branch.id = :branchId and item.active = true
            """)
    List<BranchMenuItem> findCatalog(Long branchId);

    @Query("""
            select bmi from BranchMenuItem bmi
            join fetch bmi.menuItem item
            join fetch item.category
            where bmi.branch.id = :branchId and item.id = :menuItemId
            """)
    Optional<BranchMenuItem> findOffering(Long branchId, Long menuItemId);

    Optional<BranchMenuItem> findByBranchIdAndMenuItemId(Long branchId, Long menuItemId);

    List<BranchMenuItem> findByMenuItemId(Long menuItemId);

    @Query("""
            select count(bmi) from BranchMenuItem bmi
            where bmi.available = true
              and bmi.menuItem.active = true
              and bmi.branch.active = true
            """)
    long countAvailableOfferings();
}
