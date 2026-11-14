package com.ticketflow.ticketservice;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.mariadb.MariaDBContainer;

@SpringBootTest
public abstract class AbstractIntegrationTest {

    @ServiceConnection   //Spring Boot lit tout seul l’URL, le user et le mot de passe du container, et configure la datasource et Kafka avec. Aucune URL à écrire à la main.
    static final MariaDBContainer mariadb = new MariaDBContainer("mariadb:11");

    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.9.1");

    static {  //ce bloque permet de démarrer 1 seule fois pout tous les tests. Et ça utilise le pattern Singleton
        mariadb.start();
        kafka.start();
    }
}
