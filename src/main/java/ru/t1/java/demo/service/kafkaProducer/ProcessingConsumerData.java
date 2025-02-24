package ru.t1.java.demo.service.kafkaProducer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.t1.java.demo.configuration.kafkaProducer.KafkaCreateProducer;
import java.util.UUID;

@Slf4j
@Service
public class ProcessingConsumerData {

    private final KafkaCreateProducer kafkaCreateProducer;

    @Autowired
    public ProcessingConsumerData(KafkaCreateProducer kafkaCreateProducer){
        this.kafkaCreateProducer = kafkaCreateProducer;
    }

    public void setDataToTopic(String topicName, String json) {
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, UUID.randomUUID().toString(), json);
        setProducerRecord(producerRecord);
    }

    public void setProducerRecord(ProducerRecord<String, String> producerRecord){
        KafkaProducer<String, String> kafkaProducer = kafkaCreateProducer.getKafkaProducer();

        try {
            RecordMetadata recordMetadata = kafkaProducer.send(producerRecord,
                ((metadata, exception) -> {
                    if (exception != null){
                        log.info("Ошибка при отправлении сообщения {}", exception.getMessage().toUpperCase());
                    } else {
                        log.info("Сообщение отправлено в Kafka {}", metadata.partition());
                    }
                })).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
