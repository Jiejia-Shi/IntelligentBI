package com.yupi.springbootinit.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.util.StringUtil;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GptManager {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String doChat(String prompt) {
        final String url = "https://api.openai.com/v1/chat/completions";
        final String apiKey = System.getenv("OPENAI_API_KEY");
        final String model = "gpt-4o-mini";
        final String assistantSet = "You are a professional data analyst. Please help me draw the data chart, analyse data and give result based on analysis goal and data. " +
                "Firstly, you should draw a chart, please give me the according echarts option code without anything else. The chart type should based on the given chart type. " +
                "Then, you need to output *****" +
                "Finally, you should analyse the data based on analysis goal and output a paragraph of analysis result, try to be specific and short.";

        try {
            URL obj = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

            // request headers
            connection.setRequestMethod("POST");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("API Key is empty");
            }
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json");

            // request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            // creat message list
            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            Map<String, String> developerMessage = new HashMap<>();
            developerMessage.put("role", "developer");
            developerMessage.put("content", assistantSet);
            messages.add(developerMessage);

            requestBody.put("messages", messages);

            // convert Map to JSON String
            String body = objectMapper.writeValueAsString(requestBody);

            connection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
            writer.write(body);
            writer.flush();
            writer.close();

            // response from gpt
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            StringBuilder response = new StringBuilder();

            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            br.close();

            return extractMessageFromJSONResponse(response.toString());

        } catch (MalformedURLException e) {
            throw new RuntimeException("error in requesting gpt");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String extractMessageFromJSONResponse(String response) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(response);

        // extract content field from choices[0].message.content
        JsonNode choiceNode = rootNode.path("choices");
        if (choiceNode.isArray() && !choiceNode.isEmpty()) {
            JsonNode firstChoice = choiceNode.get(0);
            return firstChoice.path("message").path("content").asText();
        } else {
            throw new RuntimeException("gpt request error: no content");
        }

//        int start = response.indexOf("content")+ 11;
//        int end = response.indexOf("\"", start);
//        return response.substring(start, end);
    }


    public static void main(String[] args) {
        GptManager gptManager = new GptManager();
        System.out.println(gptManager.doChat("goal: analyse the trend of user number, data: day1, 100, day2, 200, day3, 130, day4, 122"));
    }


}
