package com.santiago_tumbaco.identity.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.santiago_tumbaco.identity.domain.model.Person;

public interface PersonRepository extends JpaRepository<Person,Long> {
    Optional<Person> findByEmail(String email);
}
