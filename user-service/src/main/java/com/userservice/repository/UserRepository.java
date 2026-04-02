package com.userservice.repository;


import com.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsn(String usn);
    boolean existsByUsn(String usn);
    List<User> findByUsnIn(List<String> usns);
}