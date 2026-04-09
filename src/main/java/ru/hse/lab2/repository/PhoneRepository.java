package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.lab2.entity.Phone;

public interface PhoneRepository extends JpaRepository<Phone, Long> {
}