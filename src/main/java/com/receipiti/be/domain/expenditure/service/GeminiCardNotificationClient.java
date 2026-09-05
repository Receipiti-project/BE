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
public class GeminiCardNotificationClient {

    private static final String MODEL = "gemini-2.5-flash";
    private static final GenerateContentConfig GENERATION_CONFIG = GenerateContentConfig.builder()
            .temperature(0.0F)
            .responseMimeType(MediaType.APPLICATION_JSON_VALUE)
            .responseSchema(responseSchema())
            .build();

    private final Client client;

    public GeminiCardNotificationClient(Client client) {
        this.client = client;
    }

    public String analyze(String prompt, byte[] imageBytes, String contentType) {
        Content content = Content.fromParts(
                Part.fromText(prompt),
                Part.fromBytes(imageBytes, contentType)
        );
        GenerateContentResponse response = client.models.generateContent(
                MODEL,
                content,
                GENERATION_CONFIG
        );
        return response.text();
    }

    private static Schema responseSchema() {
        return Schema.builder()
                .type(Type.Known.OBJECT)
                .properties(Map.of(
                        "paymentNotification", schema(Type.Known.BOOLEAN),
                        "cardCompany", schema(Type.Known.STRING),
                        "storeName", schema(Type.Known.STRING),
                        "amount", schema(Type.Known.INTEGER),
                        "paymentDateTime", schema(Type.Known.STRING),
                        "currency", schema(Type.Known.STRING),
                        "approvalStatus", Schema.builder()
                                .type(Type.Known.STRING)
                                .enum_("APPROVED", "CANCELED", "UNKNOWN")
                                .build(),
                        "confidence", Schema.builder()
                                .type(Type.Known.NUMBER)
                                .minimum(0.0)
                                .maximum(1.0)
                                .build()
                ))
                .required(
                        "paymentNotification", "cardCompany", "storeName", "amount",
                        "paymentDateTime", "currency", "approvalStatus", "confidence"
                )
                .build();
    }

    private static Schema schema(Type.Known type) {
        return Schema.builder().type(type).build();
    }
}
