package com.ticketflow.notificationservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mariadb.MariaDBContainer;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MariaDBContainer mariadb = new MariaDBContainer("mariadb:11");

    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.9.1");

    static {
        mariadb.start();
        kafka.start();
    }
}