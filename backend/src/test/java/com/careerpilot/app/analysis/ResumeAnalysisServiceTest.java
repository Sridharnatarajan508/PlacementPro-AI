package com.careerpilot.app.analysis;

import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.*;
import com.careerpilot.app.entity.Resume;
import com.careerpilot.app.exception.AIServiceException;
import com.careerpilot.app.exception.PdfExtractionException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.GeminiService;
import com.careerpilot.app.service.ResumeAnalysisService;
import com.careerpilot.app.service.ResumeService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:analysistestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-analysis-uploads",
    "app.gemini.api-key=test_mock_gemini_key"
})
@Transactional
public class ResumeAnalysisServiceTest {

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private AuthService authService;

    @MockBean
    private GeminiService geminiService;

    private String user1Email = "analysiscandidate1@example.com";
    private String user2Email = "analysiscandidate2@example.com";
    private Long user1ResumeId;

    @BeforeEach
    void setUp() throws Exception {
        RegisterRequest user1 = new RegisterRequest();
        user1.setName("Candidate One");
        user1.setEmail(user1Email);
        user1.setPassword("Password123");
        authService.register(user1);

        RegisterRequest user2 = new RegisterRequest();
        user2.setName("Candidate Two");
        user2.setEmail(user2Email);
        user2.setPassword("Password123");
        authService.register(user2);

        // Upload sample resume for User 1
        byte[] pdfBytes = createSamplePdfBytes("John Developer - Experienced Java, Spring Boot, React, and SQL engineer.");
        MockMultipartFile file = new MockMultipartFile("file", "john_resume.pdf", "application/pdf", pdfBytes);
        ResumeResponse uploaded = resumeService.uploadResume(user1Email, file);
        user1ResumeId = uploaded.getId();
    }

    private byte[] createSamplePdfBytes(String textContent) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(textContent);
                contentStream.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    private ResumeAnalysisDto mockGeminiAnalysisDto() {
        return ResumeAnalysisDto.builder()
                .overallScore(85)
                .atsScore(80)
                .summary("Candidate demonstrates strong Java and Spring Boot expertise.")
                .strengths(List.of("Strong Java background", "Full stack capability"))
                .weaknesses(List.of("Lacks Docker containerization", "No cloud certifications"))
                .detectedSkills(List.of(
                        new DetectedSkillDto("Java", "Programming Languages", 0.95),
                        new DetectedSkillDto("Spring Boot", "Frameworks", 0.90)
                ))
                .missingSkills(List.of(
                        new MissingSkillDto("Docker", "Required for cloud deployment", "HIGH")
                ))
                .suggestions(List.of("Add measurable metrics to project accomplishments"))
                .sectionAnalysis(new SectionAnalysisDto("Good summary", "Relevant B.Tech degree", "Strong Java skills", "Good web projects", "No formal experience yet", "None"))
                .build();
    }

    @Test
    void analyzeResume_Success() {
        Mockito.when(geminiService.analyzeResumeText(anyString(), any(), any()))
                .thenReturn(mockGeminiAnalysisDto());

        ResumeAnalysisResponse response = resumeAnalysisService.analyzeResume(user1Email, user1ResumeId);

        assertNotNull(response.getId());
        assertEquals(user1ResumeId, response.getResumeId());
        assertEquals(85, response.getOverallScore());
        assertEquals(80, response.getAtsScore());
        assertEquals(2, response.getStrengths().size());
        assertEquals("Java", response.getDetectedSkills().get(0).getName());
        assertEquals("HIGH", response.getMissingSkills().get(0).getPriority());
    }

    @Test
    void getAnalysisForResume_CachedResponse_DoesNotCallGeminiAgain() {
        Mockito.when(geminiService.analyzeResumeText(anyString(), any(), any()))
                .thenReturn(mockGeminiAnalysisDto());

        // Perform first analysis
        resumeAnalysisService.analyzeResume(user1Email, user1ResumeId);

        // Fetch cached analysis
        ResumeAnalysisResponse cached = resumeAnalysisService.getAnalysisForResume(user1Email, user1ResumeId);

        assertNotNull(cached);
        assertEquals(85, cached.getOverallScore());
        // Verify Gemini was called only ONCE during initial analysis
        Mockito.verify(geminiService, Mockito.times(1)).analyzeResumeText(anyString(), any(), any());
    }

    @Test
    void resumeAnalysisOwnership_UserTwoCannotAccessOrAnalyzeUserOneResume() {
        Mockito.when(geminiService.analyzeResumeText(anyString(), any(), any()))
                .thenReturn(mockGeminiAnalysisDto());

        // User 2 trying to analyze User 1's resume
        assertThrows(ResourceNotFoundException.class, () ->
                resumeAnalysisService.analyzeResume(user2Email, user1ResumeId));

        // User 1 analyzes own resume
        resumeAnalysisService.analyzeResume(user1Email, user1ResumeId);

        // User 2 trying to view User 1's analysis
        assertThrows(ResourceNotFoundException.class, () ->
                resumeAnalysisService.getAnalysisForResume(user2Email, user1ResumeId));
    }

    @Test
    void analyzeResume_GeminiServiceError_ThrowsAIServiceException() {
        Mockito.when(geminiService.analyzeResumeText(anyString(), any(), any()))
                .thenThrow(new AIServiceException("Gemini API key authorization failed."));

        assertThrows(AIServiceException.class, () ->
                resumeAnalysisService.analyzeResume(user1Email, user1ResumeId));
    }
}
