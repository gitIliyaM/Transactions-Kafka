package ru.t1.java.demo.model;

import jakarta.persistence.*;
import lombok.*;
import ru.t1.java.demo.enums.account.*;

@Getter
@Setter
@Entity
@Table(name = "ccount_kafka_db")
public class AccountKafkaDB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "idClient", columnDefinition = "INTEGER NOT NULL")
    private Integer idClient;

    @Column(name = "idAccount", columnDefinition = "INTEGER NOT NULL")
    private Integer idAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "accountType", columnDefinition = "TEXT NOT NULL")
    private TypeAccount typeAccount;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "statusAccounts", columnDefinition = "TEXT NOT NULL")
    private StatusAccounts statusAccounts;

    @Column(name = "balance", columnDefinition = "DOUBLE PRECISION NOT NULL")
    private Double balance;

    @Column(name = "frozenAmount")
    private Double  frozenAmount;

}
