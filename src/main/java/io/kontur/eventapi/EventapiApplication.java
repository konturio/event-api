package io.kontur.eventapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@MapperScan("io.kontur.eventapi.dao.mapper")
public class EventapiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventapiApplication.class, args);
	}

}
