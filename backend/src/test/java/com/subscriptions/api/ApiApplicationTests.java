package com.subscriptions.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.mail.host=smtp.gmail.com",
        "spring.mail.port=587",
        "spring.mail.username=test@example.com",
        "spring.mail.password=test",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
