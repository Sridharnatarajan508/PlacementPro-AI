package com.careerpilot.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes", indexes = {
    @Index(name = "idx_resume_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * Path to the stored PDF on disk (relative to upload dir).
     * Never expose this path directly to the frontend.
     * Designed for future swap to cloud storage URL.
     */
    @Column(name = "file_path")
    private String filePath;

    /**
     * Text extracted from the PDF via Apache PDFBox.
     * Stored as LONGTEXT to handle large resumes.
     */
    @Column(name = "extracted_text", columnDefinition = "LONGTEXT")
    private String extractedText;

    @Column(name = "extraction_status", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ExtractionStatus extractionStatus = ExtractionStatus.PENDING;

    @Column(name = "extraction_message")
    private String extractionMessage;

    @OneToOne(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private ResumeAnalysis analysis;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ExtractionStatus {
        PENDING,
        SUCCESS,
        PARTIAL,       // Some text extracted but may be incomplete
        FAILED,        // Could not extract any text
        NO_TEXT        // PDF contains no extractable text (e.g. image-only)
    }
}
