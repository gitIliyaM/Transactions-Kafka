package ru.t1.java.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.t1.java.demo.aop.AroundDataProcessorService;
import ru.t1.java.demo.service.postParser.*;
import ru.t1.java.demo.service.jsonParser.*;
import ru.t1.java.demo.dto.*;
import ru.t1.java.demo.model.*;
import ru.t1.java.demo.repository.*;
import java.util.*;

@Service
public class DataProcessorService {

    private final AccountJsonFileReader accountJsonFileReader;
    private final TransactionJsonFileReader transactionJsonFileReader;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountPostStringReader accountPostStringReader;
    private final TransactionPostStringReader transactionPostStringReader;
    private final AccountKafkaRepository accountKafkaRepository;
    private final TransactionKafkaRepository transactionKafkaRepository;

    @Autowired()
    public DataProcessorService(
        AccountJsonFileReader accountJsonFileReader,
        TransactionJsonFileReader transactionJsonFileReader,
        AccountRepository accountRepository,
        TransactionRepository transactionRepository,
        AccountPostStringReader accountPostStringReader,
        TransactionPostStringReader transactionPostStringReader,
        AccountKafkaRepository accountKafkaRepository,
        TransactionKafkaRepository transactionKafkaRepository
    ) {
        this.accountJsonFileReader = accountJsonFileReader;
        this.transactionJsonFileReader = transactionJsonFileReader;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountPostStringReader = accountPostStringReader;
        this.transactionPostStringReader = transactionPostStringReader;
        this.accountKafkaRepository = accountKafkaRepository;
        this.transactionKafkaRepository = transactionKafkaRepository;
    }

    @AroundDataProcessorService
    public List<AccountDTO> createAccountDTOFromFile() {

        AccountDTO[] arrayAccountDTO = accountJsonFileReader.arrayAccountDTO();
        List<AccountDTO> accountDTOList = new ArrayList<>();

        if(arrayAccountDTO != null){
            for(AccountDTO account: arrayAccountDTO){
                account.setIdClient(account.getIdClient());
                account.setTypeAccount(account.getTypeAccount());
                account.setBalance(account.getBalance());
                accountDTOList.add(account);
            }
        } else {
            throw new RuntimeException("файл пустой");
        }
        return accountDTOList;
    }

    @AroundDataProcessorService
    public List<TransactionDTO> createTransactionDTOFromFile() {

        TransactionDTO[] arrayTransactionDTO = transactionJsonFileReader.arrayTransactionDTO();
        List<TransactionDTO> transactionDTOList = new ArrayList<>();

        if(arrayTransactionDTO != null){
            for(TransactionDTO transaction: arrayTransactionDTO){
                transaction.setIdClient(transaction.getIdClient());
                transaction.setAmountTransaction(transaction.getAmountTransaction());
                transaction.setTimeTransaction(transaction.getTimeTransaction());
                transactionDTOList.add(transaction);
            }
        } else {
            throw new RuntimeException("файл пустой");
        }
        return transactionDTOList;
    }

    @AroundDataProcessorService
    public List<Accounts> createAccountFromFile() {

        List<Accounts> accountsList = accountJsonFileReader.arrayListAccount();

        for(Accounts accounts : accountsList){
            try{
                accountRepository.saveAndFlush(accounts);
            } catch (RuntimeException ex){
                throw new RuntimeException(ex);
            }
        }
        return accountsList;
    }

    public List<AccountKafkaDB> createKafkaAccountFromFile() {

        List<AccountKafkaDB> accountsList = accountJsonFileReader.arrayListAccountKafkaDB();

        for(AccountKafkaDB accounts : accountsList){
            try{
                accountKafkaRepository.saveAndFlush(accounts);
            } catch (RuntimeException ex){
                throw new RuntimeException(ex);
            }
        }
        return accountsList;
    }

    @AroundDataProcessorService
    public List<Transactions> createTransactionFromFile() {

        List<Transactions> transactionsList = transactionJsonFileReader.arrayaListTransaction();

        for(Transactions transactions : transactionsList){
            try{
                transactionRepository.saveAndFlush(transactions);
            } catch (RuntimeException ex){
                throw new RuntimeException(ex);
            }
        }
        return transactionsList;
    }

    public List<TransactionKafkaDB> createKafkaTransactionFromFile() {

        List<TransactionKafkaDB> transactionsList = transactionJsonFileReader.arrayListTransactionKafkaDB();

        for(TransactionKafkaDB transaction : transactionsList){
            try{
                transactionKafkaRepository.saveAndFlush(transaction);
            } catch (RuntimeException ex){
                throw new RuntimeException(ex);
            }
        }
        return transactionsList;
    }

    @AroundDataProcessorService
    public List<Accounts> createAccountPost(String accountPost)  {

        List<Accounts> accountsList = accountPostStringReader.accountList(accountPost);

        for(Accounts accounts : accountsList){
            try{
                accountRepository.saveAndFlush(accounts);
            } catch (Exception ex){
                throw new RuntimeException(ex);
            }
        }
        return accountsList;
    }

    @AroundDataProcessorService
    public List<Transactions> createTransactionPost(String transactionPost)  {

        List<Transactions> transactionsList = transactionPostStringReader.transactionList(transactionPost);

        for(Transactions transactions : transactionsList){
            try{
                transactionRepository.saveAndFlush(transactions);
            } catch (RuntimeException ex){
                throw new RuntimeException(ex);
            }
        }
        return transactionsList;
    }
}
