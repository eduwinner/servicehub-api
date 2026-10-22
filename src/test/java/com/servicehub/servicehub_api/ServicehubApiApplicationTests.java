package com.servicehub.servicehub_api;

import com.servicehub.ServicehubApiApplication;
import com.servicehub.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = ServicehubApiApplication.class)
@Import(TestcontainersConfiguration.class)
class ServicehubApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
