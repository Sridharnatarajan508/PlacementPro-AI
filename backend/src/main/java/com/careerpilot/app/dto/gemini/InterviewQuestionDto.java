package com.careerpilot.app.dto.gemini;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewQuestionDto {

    private String question;
    private String category; // TECHNICAL, BEHAVIORAL, HR, DSA, SYSTEM_DESIGN, PROJECT
    private String difficulty; // EASY, MEDIUM, HARD
    private List<String> expectedTopics;
}
