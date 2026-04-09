package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.lab2.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}