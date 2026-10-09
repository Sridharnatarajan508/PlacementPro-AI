package com.careerpilot.app.dto.response;

import com.careerpilot.app.entity.Resume;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeResponse {

    private Long id;
    private String fileName;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String extractionStatus;
    private String extractionMessage;
    private Integer extractedTextLength;
    private String extractedText;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;

    public static ResumeResponse from(Resume resume) {
        return ResumeResponse.builder()
                .id(resume.getId())
                .fileName(resume.getFileName())
                .originalFileName(resume.getOriginalFileName())
                .fileType(resume.getFileType())
                .fileSize(resume.getFileSize())
                .fileSizeFormatted(formatFileSize(resume.getFileSize()))
                .extractionStatus(resume.getExtractionStatus().name())
                .extractionMessage(resume.getExtractionMessage())
                .extractedTextLength(resume.getExtractedText() != null
                        ? resume.getExtractedText().length() : 0)
                .extractedText(resume.getExtractedText())
                .uploadedAt(resume.getUploadedAt())
                .updatedAt(resume.getUpdatedAt())
                .build();
    }

    /** Returns a summary without the (potentially large) extracted text. */
    public static ResumeResponse summary(Resume resume) {
        ResumeResponse r = from(resume);
        r.setExtractedText(null); // omit bulk text from list responses
        return r;
    }

    private static String formatFileSize(Long bytes) {
        if (bytes == null) return "Unknown";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
