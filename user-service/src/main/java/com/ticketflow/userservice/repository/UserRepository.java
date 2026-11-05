package com.ticketflow.userservice.repository;

import com.ticketflow.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
