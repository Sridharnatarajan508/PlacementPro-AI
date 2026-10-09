package com.careerpilot.app.resume;

import com.careerpilot.app.dto.request.RegisterRequest;
import com.careerpilot.app.dto.response.AuthResponse;
import com.careerpilot.app.dto.response.ResumeResponse;
import com.careerpilot.app.exception.FileValidationException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.service.AuthService;
import com.careerpilot.app.service.ResumeService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:resumetestdb;MODE=MySQL;DB_CLOSE_DELAY=-1;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
    "app.jwt.expiration=86400000",
    "app.resume.upload-dir=target/test-resume-uploads"
})
@Transactional
public class ResumeServiceTest {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private AuthService authService;

    private String user1Email = "user1@example.com";
    private String user2Email = "user2@example.com";

    @BeforeEach
    void setUp() {
        RegisterRequest user1 = new RegisterRequest();
        user1.setName("User One");
        user1.setEmail(user1Email);
        user1.setPassword("Password123");
        authService.register(user1);

        RegisterRequest user2 = new RegisterRequest();
        user2.setName("User Two");
        user2.setEmail(user2Email);
        user2.setPassword("Password123");
        authService.register(user2);
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

    @Test
    void uploadResume_SuccessAndPdfBoxExtraction() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes("Jane Doe - Senior Java Engineer with Spring Boot experience.");
        MockMultipartFile file = new MockMultipartFile(
                "file", "jane_resume.pdf", "application/pdf", pdfBytes);

        ResumeResponse response = resumeService.uploadResume(user1Email, file);

        assertNotNull(response.getId());
        assertEquals("jane_resume.pdf", response.getOriginalFileName());
        assertEquals("SUCCESS", response.getExtractionStatus());
        assertTrue(response.getExtractedText().contains("Jane Doe"));
        assertTrue(response.getExtractedText().contains("Spring Boot"));
    }

    @Test
    void uploadResume_RejectNonPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "malicious.exe", "application/octet-stream", "fake content".getBytes());

        assertThrows(FileValidationException.class, () -> resumeService.uploadResume(user1Email, file));
    }

    @Test
    void uploadResume_RejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThrows(FileValidationException.class, () -> resumeService.uploadResume(user1Email, file));
    }

    @Test
    void uploadResume_RejectOversizedFile() {
        byte[] bigBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "huge.pdf", "application/pdf", bigBytes);

        assertThrows(FileValidationException.class, () -> resumeService.uploadResume(user1Email, file));
    }

    @Test
    void resumeOwnership_UserTwoCannotAccessUserOneResume() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes("Secret Resume Content");
        MockMultipartFile file = new MockMultipartFile("file", "secret.pdf", "application/pdf", pdfBytes);

        ResumeResponse uploadRes = resumeService.uploadResume(user1Email, file);
        Long resumeId = uploadRes.getId();

        // User2 attempting to fetch User1's resume by ID
        assertThrows(ResourceNotFoundException.class, () -> resumeService.getResumeById(user2Email, resumeId));

        // User2 attempting to delete User1's resume
        assertThrows(ResourceNotFoundException.class, () -> resumeService.deleteResume(user2Email, resumeId));
    }

    @Test
    void deleteResume_Success() throws Exception {
        byte[] pdfBytes = createSamplePdfBytes("Resume to be deleted");
        MockMultipartFile file = new MockMultipartFile("file", "todelete.pdf", "application/pdf", pdfBytes);

        ResumeResponse uploadRes = resumeService.uploadResume(user1Email, file);
        Long resumeId = uploadRes.getId();

        resumeService.deleteResume(user1Email, resumeId);

        assertThrows(ResourceNotFoundException.class, () -> resumeService.getResumeById(user1Email, resumeId));
    }
}
