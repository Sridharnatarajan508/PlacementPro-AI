package com.careerpilot.app.service;

import com.careerpilot.app.dto.response.ResumeResponse;
import com.careerpilot.app.entity.Resume;
import com.careerpilot.app.entity.Resume.ExtractionStatus;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.FileValidationException;
import com.careerpilot.app.exception.PdfExtractionException;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.ResumeRepository;
import com.careerpilot.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;

    @Value("${app.resume.upload-dir:${user.home}/career-copilot-uploads}")
    private String uploadDirSetting;

    @Value("${app.resume.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Transactional
    public ResumeResponse uploadResume(String userEmail, MultipartFile file) {
        User user = findUserByEmail(userEmail);
        validateFile(file);

        String originalFileName = Objects.requireNonNullElse(file.getOriginalFilename(), "resume.pdf");
        String sanitizedFileName = sanitizeFileName(originalFileName);
        String storedFileName = UUID.randomUUID().toString() + "_" + sanitizedFileName;

        Path uploadPath = getUploadPath();
        Path filePath = uploadPath.resolve(storedFileName);

        // Extract text first using PDFBox to verify validity
        ExtractedContent extractedContent = extractTextFromPdf(file);

        // Save file to storage
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save file to storage: {}", e.getMessage());
            throw new FileValidationException("Could not store file on server. Please try again.");
        }

        Resume resume = Resume.builder()
                .user(user)
                .fileName(storedFileName)
                .originalFileName(originalFileName)
                .fileType(file.getContentType() != null ? file.getContentType() : "application/pdf")
                .fileSize(file.getSize())
                .filePath(filePath.toString())
                .extractedText(extractedContent.text)
                .extractionStatus(extractedContent.status)
                .extractionMessage(extractedContent.message)
                .build();

        resume = resumeRepository.save(resume);
        log.info("Resume saved for user {}: {}", userEmail, resume.getId());

        return ResumeResponse.from(resume);
    }

    public List<ResumeResponse> getAllResumesForUser(String userEmail) {
        User user = findUserByEmail(userEmail);
        return resumeRepository.findByUserOrderByUploadedAtDesc(user)
                .stream()
                .map(ResumeResponse::summary)
                .collect(Collectors.toList());
    }

    public ResumeResponse getLatestResumeForUser(String userEmail) {
        User user = findUserByEmail(userEmail);
        Resume resume = resumeRepository.findFirstByUserOrderByUploadedAtDesc(user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "user", userEmail));
        return ResumeResponse.from(resume);
    }

    public ResumeResponse getResumeById(String userEmail, Long resumeId) {
        User user = findUserByEmail(userEmail);
        Resume resume = resumeRepository.findByIdAndUser(resumeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));
        return ResumeResponse.from(resume);
    }

    @Transactional
    public void deleteResume(String userEmail, Long resumeId) {
        User user = findUserByEmail(userEmail);
        Resume resume = resumeRepository.findByIdAndUser(resumeId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", "id", resumeId));

        // Delete physical file
        if (resume.getFilePath() != null) {
            try {
                Path path = Paths.get(resume.getFilePath());
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.warn("Failed to delete physical resume file at {}: {}", resume.getFilePath(), e.getMessage());
            }
        }

        resumeRepository.delete(resume);
        log.info("Deleted resume ID {} for user {}", resumeId, userEmail);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileValidationException("Uploaded file is empty. Please select a valid PDF file.");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new FileValidationException("File size exceeds the maximum limit of 5 MB.");
        }

        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean isPdfMime = contentType != null && contentType.equalsIgnoreCase("application/pdf");
        boolean isPdfExt = originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf");

        if (!isPdfMime && !isPdfExt) {
            throw new FileValidationException("Invalid file type. Only PDF documents are allowed.");
        }
    }

    private ExtractedContent extractTextFromPdf(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            if (document.isEncrypted()) {
                throw new PdfExtractionException("The uploaded PDF is password-protected or encrypted. Please upload an unencrypted PDF.");
            }

            PDFTextStripper pdfStripper = new PDFTextStripper();
            String rawText = pdfStripper.getText(document);

            if (rawText == null || rawText.trim().isEmpty()) {
                return new ExtractedContent(
                        "",
                        ExtractionStatus.NO_TEXT,
                        "PDF contains no extractable text (it may be a scanned image or empty)."
                );
            }

            // Clean excessive blank lines / whitespace
            String cleanedText = rawText.replaceAll("\r\n", "\n")
                    .replaceAll("\n{3,}", "\n\n")
                    .trim();

            return new ExtractedContent(
                    cleanedText,
                    ExtractionStatus.SUCCESS,
                    "Text extracted successfully using PDFBox (" + cleanedText.length() + " chars)."
            );

        } catch (PdfExtractionException e) {
            throw e;
        } catch (IOException e) {
            log.error("Error reading PDF with PDFBox: {}", e.getMessage());
            throw new PdfExtractionException("Corrupted or unreadable PDF file. " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error extracting PDF text: {}", e.getMessage());
            throw new PdfExtractionException("Failed to process PDF document: " + e.getMessage());
        }
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path getUploadPath() {
        Path uploadPath = Paths.get(uploadDirSetting);
        if (!Files.exists(uploadPath)) {
            try {
                Files.createDirectories(uploadPath);
            } catch (IOException e) {
                log.error("Failed to create upload directory {}: {}", uploadDirSetting, e.getMessage());
                throw new FileValidationException("Server storage configuration error.");
            }
        }
        return uploadPath;
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private static class ExtractedContent {
        final String text;
        final ExtractionStatus status;
        final String message;

        ExtractedContent(String text, ExtractionStatus status, String message) {
            this.text = text;
            this.status = status;
            this.message = message;
        }
    }
}
