package com.da.billiards.user;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    long countByRole(Role role);

    @Query("""
            select u from User u
            where :q is null
               or lower(u.fullName) like lower(concat('%', :q, '%'))
               or lower(u.email) like lower(concat('%', :q, '%'))
               or u.phone like concat('%', :q, '%')
            """)
    Page<User> search(@Param("q") String q, Pageable pageable);
}
