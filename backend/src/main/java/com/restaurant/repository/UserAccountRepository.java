package com.restaurant.repository;

import com.restaurant.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Query("select u from UserAccount u left join fetch u.branch where lower(u.email) = lower(:email)")
    Optional<UserAccount> findByEmailWithBranch(String email);

    @Query("select u from UserAccount u left join fetch u.branch where u.id = :id")
    Optional<UserAccount> findWithBranchById(Long id);

    boolean existsByEmailIgnoreCase(String email);
}
