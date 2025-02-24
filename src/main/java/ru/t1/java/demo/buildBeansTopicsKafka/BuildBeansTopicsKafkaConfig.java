package ru.t1.java.demo.buildBeansTopicsKafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class BuildBeansTopicsKafkaConfig {

    @Bean
    public NewTopic t1_demo_metrics() {
        return TopicBuilder.name("t1_demo_metrics")
            .partitions(2)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic t1_demo_accounts () {
        return TopicBuilder.name("t1_demo_accounts")
            .partitions(2)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic t1_demo_transactions () {
        return TopicBuilder.name("t1_demo_transactions")
            .partitions(2)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic t1_demo_transaction_accept () {
        return TopicBuilder.name("t1_demo_transaction_accept")
            .partitions(2)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic t1_demo_transaction_result () {
        return TopicBuilder.name("t1_demo_transaction_result")
            .partitions(2)
            .replicas(1)
            .build();
    }
}
