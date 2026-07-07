package com.receipiti.be.domain.report.dto.request;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeminiRequest {
    private List<Content> contents;

    public GeminiRequest(String text) {
        this.contents = new ArrayList<>();
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(text));
        this.contents.add(new Content(parts));
    }

    @Getter
    @Setter
    public static class Content {
        private List<Part> parts;
        public Content(List<Part> parts) { this.parts = parts; }
    }

    @Getter
    @Setter
    public static class Part {
        private String text;
        public Part(String text) { this.text = text; }
    }
}