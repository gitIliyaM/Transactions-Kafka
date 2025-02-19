package ru.t1.java.demo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.*;
import ru.t1.java.demo.configuration.kafkaConsumer.KafkaCreateConsumer;
import org.springframework.stereotype.Service;
import org.apache.kafka.clients.consumer.*;
import lombok.extern.slf4j.Slf4j;
import ru.t1.java.demo.enums.account.StatusAccounts;
import ru.t1.java.demo.enums.transaction.StatusTransactions;
import ru.t1.java.demo.model.*;
import ru.t1.java.demo.repository.*;
import ru.t1.java.demo.service.kafkaProducer.topics.T1DemoTransactionAccept;
import ru.t1.java.demo.service.kafkaProducer.topics.T1DemoTransactionBlocked;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.*;

@Slf4j
@Service
public class KafkaConsumerProcessorService {

    private final KafkaCreateConsumer kafkaCreateConsumer;
    private final AccountKafkaRepository accountKafkaRepository;
    private final TransactionKafkaRepository transactionKafkaRepository;
    private final T1DemoTransactionAccept t1DemoTransactionAccept;
    private final Map<String, List<T1DemoTransactionAccept>> transactionsT1DemoTransactionAccept = new ConcurrentHashMap<>();
    private final T1DemoTransactionBlocked t1DemoTransactionBlocked;
    private final Map<String,String> map = new HashMap<>();
    private String consumer;

    @Value("${application.max-transactions}")
    private int maxTransactions;

    @Value("${application.time-window}")
    private long timeWindow;

    @Autowired
    KafkaConsumerProcessorService(
        KafkaCreateConsumer kafkaCreateConsumer,
        AccountKafkaRepository accountKafkaRepository,
        TransactionKafkaRepository transactionKafkaRepository,
        T1DemoTransactionAccept t1DemoTransactionAccept,
        T1DemoTransactionBlocked t1DemoTransactionBlocked
    )
    {
        this.kafkaCreateConsumer = kafkaCreateConsumer;
        this.accountKafkaRepository = accountKafkaRepository;
        this.transactionKafkaRepository = transactionKafkaRepository;
        this.t1DemoTransactionAccept = t1DemoTransactionAccept;
        this.t1DemoTransactionBlocked = t1DemoTransactionBlocked;
    }

    public String t1DemoAccountsConsumer(){

        String t1DemoAccounts = "t1_demo_accounts";

        for(ConsumerRecord<String, String> consumerRecord: consumerRecords(t1DemoAccounts)){
            consumer = setMapToString(consumerRecord);  // из карты создаем строку
        }
        log.info("t1_demo_accounts: {}", consumer.toUpperCase());
        return consumer;
    }

    public String t1DemoTransactionConsumer(){

        String t1DemoTransactions = "t1_demo_transactions";

        for (ConsumerRecord<String, String> consumerRecord : consumerRecords(t1DemoTransactions)) {
            List<AccountKafkaDB> accountKafkaDBList = accountKafkaRepository.findAll();
            for(AccountKafkaDB accountKafkaDB: accountKafkaDBList){
                if(accountKafkaDB.getStatusAccounts().equals(StatusAccounts.OPEN) && new JSONObject(consumerRecord.value()).getString("statusTransactions").equals("REQUESTED")){
                    if(accountKafkaDB.getIdClient().equals(new JSONObject(consumerRecord.value()).getInt("idClient")) && accountKafkaDB.getIdAccount().equals(new JSONObject(consumerRecord.value()).getInt("idTransaction"))){
                        accountKafkaDB.setBalance(accountKafkaDB.getBalance() + new JSONObject(consumerRecord.value()).getInt("amountTransaction"));
                        accountKafkaRepository.saveAndFlush(accountKafkaDB);
                        ObjectMapper objectMapper = new ObjectMapper();
                        TransactionKafkaDB transactionKafkaDB = null;
                        try {
                            transactionKafkaDB = objectMapper.readValue(consumerRecord.value(), TransactionKafkaDB.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        transactionKafkaDB.setTimeTransaction(System.currentTimeMillis());
                        transactionKafkaRepository.saveAndFlush(transactionKafkaDB);
                        t1DemoTransactionAccept.setT1DemoTransactionAccept(accountKafkaDB, consumerRecord, System.currentTimeMillis()); // отправление в топик T1DemoTransactionAccept
                    }
                }
            }
            consumer = setMapToString(consumerRecord);
        }
        log.info("t1_demo_transactions: {}", consumer);
        return consumer;
    }

    public String listenerTransactionAccept() {

        String t1DemoTransactionAccept = "t1_demo_transaction_accept";
        String t1DemoTransactionResult = "t1_demo_transaction_result";

        for(ConsumerRecord<String, String> consumerRecord: consumerRecords(t1DemoTransactionAccept)){
            Integer clientId = new JSONObject(consumerRecord.value()).getInt("idClient");
            Integer accountId = new JSONObject(consumerRecord.value()).getInt("idAccount");
            Integer transactionId = new JSONObject(consumerRecord.value()).getInt("transactionId");
            Long timestamp = new JSONObject(consumerRecord.value()).getLong("timestamp");
            Double amount = new JSONObject(consumerRecord.value()).getDouble("transactionAmount");
            String idClientAccount = clientId + "|" + accountId;
            transactionsT1DemoTransactionAccept.computeIfAbsent(idClientAccount, k -> new ArrayList<>()).add(new T1DemoTransactionAccept(transactionId, timestamp, StatusTransactions.REQUESTED, amount));
            checkTimeStampMaxTransactions();
        }

        for(ConsumerRecord<String, String> consumerRecord: consumerRecords(t1DemoTransactionResult)){

            String status = new JSONObject(consumerRecord.value()).getString("status");
            Integer transactionId = new JSONObject(consumerRecord.value()).getInt("transactionId");
            Optional<TransactionKafkaDB> transactionKafkaDB = transactionKafkaRepository.findById(transactionId);

            if(transactionKafkaDB.isPresent()){
                switch(status){
                    case "ACCEPTED":
                        transactionKafkaDB.get().setStatusTransactions(StatusTransactions.ACCEPTED);
                        break;
                    case "BLOCKED":
                        transactionKafkaDB.get().setStatusTransactions(StatusTransactions.BLOCKED);
                        Optional<AccountKafkaDB> accountBLOCKED = accountKafkaRepository.findById(transactionKafkaDB.get().getIdTransaction());
                        if(accountBLOCKED.isPresent()){
                            accountBLOCKED.get().setStatusAccounts(StatusAccounts.BLOCKED);
                            Double frozenAmount = transactionKafkaDB.get().getAmountTransaction();
                            accountBLOCKED.get().setFrozenAmount(frozenAmount);
                            Double newBalance = accountBLOCKED.get().getBalance() - frozenAmount;
                            accountBLOCKED.get().setBalance(newBalance);
                            accountKafkaRepository.save(accountBLOCKED.get());
                        }
                        break;
                    case "REJECTED":
                        transactionKafkaDB.get().setStatusTransactions(StatusTransactions.REJECTED);
                        Optional<AccountKafkaDB> accountREJECTED = accountKafkaRepository.findById(transactionKafkaDB.get().getIdTransaction());
                        if(accountREJECTED.isPresent()){
                            accountREJECTED.get().setBalance(accountREJECTED.get().getBalance() + transactionKafkaDB.get().getAmountTransaction());
                            accountKafkaRepository.save(accountREJECTED.get());
                        }
                        break;
                    default:
                        break;
                }
                transactionKafkaRepository.save(transactionKafkaDB.get());
            }
            return "Операция выполнена";
        }
        return "Операция не выполнена";
    }

    public void checkTimeStampMaxTransactions(){

        List<T1DemoTransactionAccept> validTransactionsList = new ArrayList<>();

        for(Map.Entry<String, List<T1DemoTransactionAccept>> entry: transactionsT1DemoTransactionAccept.entrySet()){
            Long currentTimeMillis = System.currentTimeMillis();
            List<T1DemoTransactionAccept> acceptList = entry.getValue();
            for (T1DemoTransactionAccept accept : acceptList) {
                if (currentTimeMillis - accept.getTimestamp() <= timeWindow && acceptList.size() <= maxTransactions) {
                    validTransactionsList.add(accept);
                    checkDebitAmount(accept);
                } else {
                    t1DemoTransactionBlocked.setT1DemoTransactionBlocked(String.valueOf(StatusTransactions.BLOCKED), accept);

                }
            }
        }
    }

    public void checkDebitAmount(T1DemoTransactionAccept accept){

        Optional<AccountKafkaDB> accountKafkaDB = accountKafkaRepository.findById(accept.getClientId());

        if(accountKafkaDB.isPresent()){
            if(accept.getTransactionAmount() > accountKafkaDB.get().getBalance()){
                t1DemoTransactionBlocked.setT1DemoTransactionBlocked(String.valueOf(StatusTransactions.REJECTED), accept);
                log.info("Сумма транзакции больше суммы на счете {}", accountKafkaDB.get().getBalance());
            } else {
                t1DemoTransactionBlocked.setT1DemoTransactionBlocked(String.valueOf(StatusTransactions.ACCEPTED), accept);
                log.info("Транзакция выполнена {}", accountKafkaDB.get().getBalance());
            }
        } else {
            log.info("Не найден счет клиента в Базе Банных");
        }
    }

    public ConsumerRecords<String, String> consumerRecords(String t1DemoConsumerTopic){

        KafkaConsumer<String, String> consumer = kafkaCreateConsumer.getKafkaConsumer();

        switch (t1DemoConsumerTopic) {
            case "t1_demo_accounts", "t1_demo_transaction_accept", "t1_demo_transactions" -> consumer.subscribe(Pattern.compile(t1DemoConsumerTopic));
        }
        return consumer.poll(Duration.ofSeconds(10));
    }
    public String setMapToString(ConsumerRecord<String, String> consumerRecord) {

        ObjectMapper mapper = new ObjectMapper();
        map.put(consumerRecord.key(),consumerRecord.value());

        try {
            return mapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage().toUpperCase());
        }
    }
}

