package ru.t1.java.demo.aop;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.*;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.t1.java.demo.configuration.kafkaProducer.KafkaCreateProducer;
import ru.t1.java.demo.service.LogErrorDataProcessorService;

@Slf4j
@Aspect
@Component
public class LogDataSourceError {

    private final KafkaCreateProducer kafkaCreateProducer;
    private final LogErrorDataProcessorService logErrorDataProcessorService;
    private final NewTopic topicT1DemoMetrics;

    @Autowired
    LogDataSourceError(
        KafkaCreateProducer kafkaCreateProducer,
        LogErrorDataProcessorService logErrorDataProcessorService,
        @Qualifier("t1_demo_metrics") NewTopic topicT1DemoMetrics
    ){
        this.kafkaCreateProducer = kafkaCreateProducer;
        this.logErrorDataProcessorService = logErrorDataProcessorService;
        this.topicT1DemoMetrics = topicT1DemoMetrics;
    }

    @AfterThrowing(pointcut = "execution(public * ru.t1.java.demo.service.DataProcessorService.*(..))", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable ex) {

        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicT1DemoMetrics.name(), "Method Signature", joinPoint.getSignature().toString());
        producerRecord.headers().add("DATA_SOURCE", ex.getMessage().getBytes());

        try{
            KafkaProducer<String, String> kafkaProducer = kafkaCreateProducer.getKafkaProducer();
            RecordMetadata recordMetadata = kafkaProducer.send(producerRecord,
                ((metadata, exception) -> {
                    if (exception != null){
                        errorSendToKafka(metadata, joinPoint, ex);
                    } else {
                        messageSentToKafka(metadata);
                    }
                })).get();
        } catch (Exception exception) {
            errorCreateProducer(joinPoint, ex);
        }
    }

    public void errorSendToKafka(RecordMetadata recordMetadata, JoinPoint joinPoint, Throwable ex){
        log.info("Ошибка при отправке сообщения в Kafka: {}", recordMetadata.topic());
        logErrorDataProcessorService.logAfterThrowing(joinPoint, ex);
    }

    public void messageSentToKafka(RecordMetadata recordMetadata){
        log.info("Сообщение отправлено в Topic: {}", recordMetadata.topic());
        log.info("Сообщение отправлено в Partition: {}", recordMetadata.partition());
    }

    public void errorCreateProducer(JoinPoint joinPoint, Throwable ex){
        log.info("Не удалось создать Producer: {}", ex.getMessage());
        logErrorDataProcessorService.logAfterThrowing(joinPoint,ex);
    }

}
