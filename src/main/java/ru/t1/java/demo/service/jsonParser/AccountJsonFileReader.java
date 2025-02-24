package ru.t1.java.demo.service.jsonParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.t1.java.demo.dto.AccountDTO;
import ru.t1.java.demo.model.*;
import java.util.List;
import java.io.*;

@Component
public class AccountJsonFileReader {

    private final File file = new File("src/main/resources/account.json");

    public AccountDTO[] arrayAccountDTO() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(file, AccountDTO[].class);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<Accounts> arrayListAccount() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, Accounts.class));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<AccountKafkaDB> arrayListAccountKafkaDB() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            return objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, AccountKafkaDB.class));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
