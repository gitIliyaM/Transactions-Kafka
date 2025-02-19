package ru.t1.java.demo.model;

import jakarta.persistence.*;
import lombok.*;
import ru.t1.java.demo.enums.account.*;

@Getter
@Setter
@Entity
@Table(name = "accounts")
public class Accounts {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "idClient")
    private Integer idClient;

    @Column(name = "idAccount")
    private Integer idAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "accountType")
    private TypeAccount typeAccount;

    @Enumerated(value = EnumType.STRING)
    @Column(name = "statusAccounts")
    private StatusAccounts statusAccounts;

    @Column(name = "balance")
    private Double balance;

    @Column(name = "frozenAmount")
    private Double  frozenAmount;

}
