package io.kontur.eventapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@MapperScan("io.kontur.eventapi.dao.mapper")
public class EventapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventapiApplication.class, args);
	}

}
