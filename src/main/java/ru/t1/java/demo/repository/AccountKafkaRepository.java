package ru.t1.java.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.t1.java.demo.model.AccountKafkaDB;

@Repository
public interface AccountKafkaRepository extends JpaRepository<AccountKafkaDB, Integer> {

}
