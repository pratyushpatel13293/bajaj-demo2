package bajaj.demo1;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class WebhookRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {

        RestTemplate restTemplate = new RestTemplate();

        // ─────────────────────────────────────────────
        // STEP 1: Generate Webhook
        // ─────────────────────────────────────────────
        String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> body = new HashMap<>();
        body.put("name", "Pratyush Patel");           // ← YOUR FULL NAME
        body.put("regNo", "REG12347");           // ← YOUR REGISTRATION NUMBER
        body.put("email", "ppratyush545@gmail.com");   // ← YOUR EMAIL

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        System.out.println("==> Calling generateWebhook...");
        ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, request, Map.class);
        Map<String, Object> responseBody = response.getBody();

        System.out.println("==> Response: " + responseBody);

        String webhook    = (String) responseBody.get("webhook");
        String accessToken = (String) responseBody.get("accessToken");

        System.out.println("==> Webhook URL  : " + webhook);
        System.out.println("==> Access Token : " + accessToken);

        // ─────────────────────────────────────────────
        // STEP 2: Submit SQL Query
        // ─────────────────────────────────────────────
        String finalQuery =
            "SELECT p.AMOUNT AS SALARY, " +
            "CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
            "TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, " +
            "d.DEPARTMENT_NAME " +
            "FROM PAYMENTS p " +
            "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
            "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
            "WHERE DAY(p.PAYMENT_TIME) != 1 " +
            "ORDER BY p.AMOUNT DESC " +
            "LIMIT 1";

        Map<String, String> submitBody = new HashMap<>();
        submitBody.put("finalQuery", finalQuery);

        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        submitHeaders.set("Authorization", accessToken);   // raw token, no "Bearer" prefix

        HttpEntity<Map<String, String>> submitRequest = new HttpEntity<>(submitBody, submitHeaders);

        System.out.println("==> Submitting SQL to webhook...");
        try {
            ResponseEntity<String> submitResponse = restTemplate.postForEntity(webhook, submitRequest, String.class);
            System.out.println("==> Submit Status  : " + submitResponse.getStatusCode());
            System.out.println("==> Submit Response: " + submitResponse.getBody());
        } catch (Exception e) {
            // If token needs "Bearer" prefix, retry
            System.err.println("==> First attempt failed: " + e.getMessage());
            System.out.println("==> Retrying with Bearer prefix...");
            submitHeaders.set("Authorization", "Bearer " + accessToken);
            HttpEntity<Map<String, String>> retryRequest = new HttpEntity<>(submitBody, submitHeaders);
            ResponseEntity<String> retryResponse = restTemplate.postForEntity(webhook, retryRequest, String.class);
            System.out.println("==> Retry Status  : " + retryResponse.getStatusCode());
            System.out.println("==> Retry Response: " + retryResponse.getBody());
        }
    }
}
