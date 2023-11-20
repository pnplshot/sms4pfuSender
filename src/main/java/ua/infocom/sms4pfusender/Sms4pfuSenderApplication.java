package ua.infocom.sms4pfusender;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Sms4pfuSenderApplication {

	public static void main(String[] args) {
		SpringApplication.run(Sms4pfuSenderApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public CommandLineRunner run(RestApiClient restApiClient) {
		return args -> {
			restApiClient.sendMessage();
		};
	}
}