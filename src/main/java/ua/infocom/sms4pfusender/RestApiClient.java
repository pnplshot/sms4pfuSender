package ua.infocom.sms4pfusender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import com.google.common.util.concurrent.RateLimiter;


import java.io.IOException;
import java.util.List;
import java.util.Base64;


@Service
@Slf4j
public class RestApiClient {

    private final SmsRepository smsRepository;
    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String source;
    private final String contentType;
    private final String serviceType;
    private final String bearerType;
    private final String login;
    private final String password;

    public RestApiClient(
            SmsRepository smsRepository,
            RestTemplate restTemplate,
            @Value("${external.api.url}") String apiUrl,
            @Value("${external.api.source}") String source,
            @Value("${external.api.contentType}") String contentType,
            @Value("${external.api.serviceType}") String serviceType,
            @Value("${external.api.bearerType}") String bearerType,
            @Value("${authorization.login}") String login,
            @Value("${authorization.password}") String password
    ) {
        this.smsRepository = smsRepository;
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.source = source;
        this.contentType = contentType;
        this.serviceType = serviceType;
        this.bearerType = bearerType;
        this.login = login;
        this.password = password;
        log.info("RestApiClient initialized with apiUrl: {}, source: {}", apiUrl, source);
    }

    private final RateLimiter rateLimiter = RateLimiter.create(20.0); // 20 запросов в секунду

    public void sendMessage() {
        log.info("api url:"+apiUrl);
        log.info("source :"+source);

        List<Sms> messages = smsRepository.findByMsgIdIsNull();

        HttpHeaders headers = new HttpHeaders();

        // Создаем строку логина и пароля в формате "username:password"
        String credentials = login + ":" + password;

        // Кодируем логин и пароль в Base64
        String base64Credentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        // Устанавливаем заголовок Authorization
        headers.set("Authorization", "Basic " + base64Credentials);


        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info(String.valueOf("size of messages :"+messages.size()));

        for (Sms message : messages) {
            // Попытка ожидания, чтобы соответствовать ограничению на скорость
            rateLimiter.acquire();

            message.setMessage("Для отримання підтримки на оплату твердого палива відповідно до постанови КМУ від 07.11.2023 №1173 просимо до 01.12.2023 надати згоду Пенсійному фонду на передачу персональних даних одержувача та членів домогосподарства надавачам допомоги. Надіслати відповідь ТАК чи НІ");

            // Создаем тело запроса
            String requestBody = String.format("{ \"source\": \"%s\", \"destination\": \"%s\", \"contentType\": \"%s\", \"serviceType\": \"%s\", \"bearerType\": \"%s\", \"content\": \"%s\" }",
                    source, message.getNumber(), contentType, serviceType, bearerType, message.getMessage());

            // Создаем объект HttpEntity, объединяющий тело запроса и заголовки
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            log.info(String.valueOf(requestEntity));
            try {
                // Выполняем POST-запрос к внешнему API
                String response = restTemplate.postForObject(apiUrl, requestEntity, String.class);

                // Обрабатываем успешный ответ (статус код 202)
                //Sms sms = new Sms();
                //message.setStatus("success"); // sms
                message.setMsgId(response);

                // Сохраняем данные в базе данных
                smsRepository.save(message);
            } catch (HttpStatusCodeException e) {
                // Обрабатываем ошибку (статус код 400)
                String response = e.getResponseBodyAsString();
                log.info(response);
                //Sms sms = new Sms();
                //message.setStatus("error"); // sms
                message.setMsgId(response);

                // Пытаемся извлечь errorId и errorMsg из JSON-ответа
                try {
                    // Парсим JSON-ответ
                    ObjectMapper objectMapper = new ObjectMapper();
                    JsonNode jsonNode = objectMapper.readTree(response);
                    message.setMsgId(jsonNode.get("mid").asText());
                    message.setErrorId(jsonNode.get("errorId").asText());
                    message.setErrorMsg(jsonNode.get("errorMsg").asText());
                } catch (IOException ignored) {
                    // Ошибка при парсинге JSON-ответа
                }

                // Сохраняем данные в базе данных
                smsRepository.save(message);
            }
        }
    }
}