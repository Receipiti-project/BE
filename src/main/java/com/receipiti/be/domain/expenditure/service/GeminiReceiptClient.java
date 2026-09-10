package com.receipiti.be.domain.expenditure.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class GeminiReceiptClient {

    private static final String MODEL = "gemini-2.5-flash";
    private static final GenerateContentConfig CONFIG = GenerateContentConfig.builder()
            .temperature(0.0F)
            .responseMimeType(MediaType.APPLICATION_JSON_VALUE)
            .responseSchema(responseSchema())
            .build();

    private final Client client;

    public GeminiReceiptClient(Client client) {
        this.client = client;
    }

    public String analyze(String prompt, byte[] imageBytes, String contentType) {
        Content content = Content.fromParts(
                Part.fromText(prompt),
                Part.fromBytes(imageBytes, contentType)
        );
        GenerateContentResponse response = client.models.generateContent(MODEL, content, CONFIG);
        return response.text();
    }

    private static Schema responseSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "storeName", Schema.builder().type(Type.Known.STRING).build(),
                        "amount", Schema.builder().type(Type.Known.INTEGER).minimum(0.0).build(),
                        "paymentDate", Schema.builder().type(Type.Known.STRING).build(),
                        "confidence", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .minimum(0.0)
                                .maximum(1.0)
                                .build()
                ))
                .required("storeName", "amount", "paymentDate", "confidence")
                .build();
    }
}
