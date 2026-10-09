package com.careerpilot.app.config;

import com.careerpilot.app.entity.Job;
import com.careerpilot.app.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final JobRepository jobRepository;

    @Override
    public void run(String... args) {
        if (jobRepository.count() == 0) {
            log.info("Seeding realistic sample job postings into MySQL database...");

            List<Job> sampleJobs = List.of(
                Job.builder()
                    .title("Java Backend Developer")
                    .company("TechNova Solutions")
                    .location("Bengaluru, Karnataka (Hybrid)")
                    .jobType("FULL_TIME")
                    .experienceLevel("FRESHER")
                    .salaryMin(new BigDecimal("600000"))
                    .salaryMax(new BigDecimal("900000"))
                    .description("Join TechNova's core engineering team to build scalable microservices and RESTful APIs using Java 21 and Spring Boot. You will work on high-throughput distributed transaction processing systems, optimize database queries in MySQL, and implement secure JWT authentication.")
                    .requiredSkills("Java, Spring Boot, MySQL, REST API, Git")
                    .preferredSkills("Docker, Hibernate, JUnit, AWS")
                    .applicationUrl("https://example.com/careers/technova-java-backend")
                    .postedDate(LocalDate.now().minusDays(2))
                    .build(),

                Job.builder()
                    .title("Spring Boot Microservices Engineer")
                    .company("CloudPeak Systems")
                    .location("Hyderabad, Telangana (On-site)")
                    .jobType("FULL_TIME")
                    .experienceLevel("JUNIOR")
                    .salaryMin(new BigDecimal("800000"))
                    .salaryMax(new BigDecimal("1200000"))
                    .description("CloudPeak Systems is looking for a Junior Backend Engineer passionate about microservices. You will develop resilient services with Spring Cloud, integrate Kafka messaging, write automated tests, and collaborate with frontend developers using React.")
                    .requiredSkills("Java, Spring Boot, Spring Data JPA, Microservices, MySQL")
                    .preferredSkills("Kafka, Docker, Redis, Kubernetes")
                    .applicationUrl("https://example.com/careers/cloudpeak-spring-microservices")
                    .postedDate(LocalDate.now().minusDays(4))
                    .build(),

                Job.builder()
                    .title("Full Stack Java Developer")
                    .company("BrightCode Labs")
                    .location("Pune, Maharashtra (Hybrid)")
                    .jobType("FULL_TIME")
                    .experienceLevel("FRESHER")
                    .salaryMin(new BigDecimal("550000"))
                    .salaryMax(new BigDecimal("850000"))
                    .description("BrightCode Labs is building next-generation SaaS enterprise tools. As a Full Stack Java Developer, you will develop responsive web interfaces with React, robust backend endpoints with Spring Boot, and integrate relational databases.")
                    .requiredSkills("Java, React, Spring Boot, JavaScript, MySQL, HTML/CSS")
                    .preferredSkills("TypeScript, Tailwind CSS, Docker, Maven")
                    .applicationUrl("https://example.com/careers/brightcode-fullstack-java")
                    .postedDate(LocalDate.now().minusDays(1))
                    .build(),

                Job.builder()
                    .title("Junior Software Engineer (Java)")
                    .company("Nexora Tech")
                    .location("Chennai, Tamil Nadu (On-site)")
                    .jobType("FULL_TIME")
                    .experienceLevel("FRESHER")
                    .salaryMin(new BigDecimal("450000"))
                    .salaryMax(new BigDecimal("700000"))
                    .description("Nexora Tech offers an exciting environment for fresh graduates to accelerate their careers in software development. You will contribute to backend logic, write clean object-oriented Java code, and participate in code reviews and agile sprints.")
                    .requiredSkills("Java, OOP, Data Structures & Algorithms, SQL, Git")
                    .preferredSkills("Spring Boot, Spring Security, REST API")
                    .applicationUrl("https://example.com/careers/nexora-junior-software-engineer")
                    .postedDate(LocalDate.now().minusDays(5))
                    .build(),

                Job.builder()
                    .title("Backend Developer (Java & SQL)")
                    .company("DataStream Systems")
                    .location("Remote, India")
                    .jobType("FULL_TIME")
                    .experienceLevel("JUNIOR")
                    .salaryMin(new BigDecimal("700000"))
                    .salaryMax(new BigDecimal("1100000"))
                    .description("DataStream is looking for a detail-oriented Backend Developer to manage data pipelines and backend web services. Key responsibilities include creating scalable REST services, query optimization in MySQL and PostgreSQL, and implementing caching mechanisms.")
                    .requiredSkills("Java, Spring Boot, SQL, MySQL, PostgreSQL, REST API")
                    .preferredSkills("Redis, AWS S3, Spring Batch, Docker")
                    .applicationUrl("https://example.com/careers/datastream-backend-java")
                    .postedDate(LocalDate.now().minusDays(3))
                    .build(),

                Job.builder()
                    .title("Associate Software Engineer")
                    .company("InfoMatrix Technologies")
                    .location("Noida, Uttar Pradesh (Hybrid)")
                    .jobType("FULL_TIME")
                    .experienceLevel("FRESHER")
                    .salaryMin(new BigDecimal("500000"))
                    .salaryMax(new BigDecimal("750000"))
                    .description("InfoMatrix invites applications from entry-level engineers to join our digital transformation unit. You will be trained on modern cloud stacks and work hands-on with Java, Spring frameworks, and modern web application development.")
                    .requiredSkills("Java, Spring Boot, Database Management, Git, Problem Solving")
                    .preferredSkills("React, Postman, Linux basics, Unit Testing")
                    .applicationUrl("https://example.com/careers/infomatrix-associate-se")
                    .postedDate(LocalDate.now().minusDays(6))
                    .build(),

                Job.builder()
                    .title("React & Spring Boot Developer")
                    .company("WebCraft Digital")
                    .location("Bengaluru, Karnataka (On-site)")
                    .jobType("FULL_TIME")
                    .experienceLevel("JUNIOR")
                    .salaryMin(new BigDecimal("800000"))
                    .salaryMax(new BigDecimal("1300000"))
                    .description("WebCraft is creating high-impact customer portals. We need a developer proficient in both frontend React components and backend Spring Boot microservices with strong API design fundamentals.")
                    .requiredSkills("React, JavaScript, Java, Spring Boot, REST API, Tailwind CSS")
                    .preferredSkills("TypeScript, Redux/Zustand, Docker, CI/CD")
                    .applicationUrl("https://example.com/careers/webcraft-react-spring")
                    .postedDate(LocalDate.now().minusDays(2))
                    .build(),

                Job.builder()
                    .title("Java Cloud Engineer")
                    .company("ScaleSphere Cloud")
                    .location("Remote / Mumbai, Maharashtra")
                    .jobType("FULL_TIME")
                    .experienceLevel("JUNIOR")
                    .salaryMin(new BigDecimal("900000"))
                    .salaryMax(new BigDecimal("1400000"))
                    .description("ScaleSphere delivers cloud-native infrastructure for fintech clients. You will build and deploy containerized Java Spring Boot services to AWS, configure CI/CD pipelines, and implement resilient event-driven architectures.")
                    .requiredSkills("Java, Spring Boot, AWS, Docker, Microservices, SQL")
                    .preferredSkills("Kubernetes, Terraform, Kafka, Prometheus")
                    .applicationUrl("https://example.com/careers/scalesphere-java-cloud")
                    .postedDate(LocalDate.now().minusDays(7))
                    .build(),

                Job.builder()
                    .title("Software Developer Trainee")
                    .company("CodeHarbour Technologies")
                    .location("Coimbatore, Tamil Nadu (On-site)")
                    .jobType("INTERNSHIP")
                    .experienceLevel("FRESHER")
                    .salaryMin(new BigDecimal("300000"))
                    .salaryMax(new BigDecimal("500000"))
                    .description("Kickstart your tech career with CodeHarbour's 6-month developer trainee program leading to full-time placement. You will learn best software engineering practices, work on real projects using Java and modern web technologies, and receive 1-on-1 mentorship.")
                    .requiredSkills("Java, Basics of Web Development, SQL, OOP Concepts")
                    .preferredSkills("Spring Boot, Git, React basics")
                    .applicationUrl("https://example.com/careers/codeharbour-trainee")
                    .postedDate(LocalDate.now().minusDays(1))
                    .build(),

                Job.builder()
                    .title("REST API Backend Developer")
                    .company("AppBridge Solutions")
                    .location("Hyderabad, Telangana (Hybrid)")
                    .jobType("FULL_TIME")
                    .experienceLevel("FRESHER")
                    .salaryMin(new BigDecimal("600000"))
                    .salaryMax(new BigDecimal("850000"))
                    .description("AppBridge provides B2B API integration solutions. As a Backend Developer, you will design clean, versioned RESTful APIs with Spring Boot, enforce JWT authentication, write thorough Postman tests, and optimize data persistence with MySQL.")
                    .requiredSkills("Java, Spring Boot, RESTful Web Services, MySQL, JWT, Git")
                    .preferredSkills("Swagger/OpenAPI, Mockito, Redis, AWS EC2")
                    .applicationUrl("https://example.com/careers/appbridge-rest-api")
                    .postedDate(LocalDate.now().minusDays(3))
                    .build(),

                Job.builder()
                    .title("Java Full Stack Engineer")
                    .company("Synthetix Software")
                    .location("Bengaluru, Karnataka (Hybrid)")
                    .jobType("FULL_TIME")
                    .experienceLevel("JUNIOR")
                    .salaryMin(new BigDecimal("750000"))
                    .salaryMax(new BigDecimal("1100000"))
                    .description("Synthetix Software builds intelligent analytics platforms. We are seeking a Full Stack Engineer to work across the stack with Java, Spring Boot, React, and MySQL, building responsive UIs and robust backend services.")
                    .requiredSkills("Java, Spring Boot, React, JavaScript, MySQL, REST API")
                    .preferredSkills("Docker, TypeScript, Cloud Deployment, Agile")
                    .applicationUrl("https://example.com/careers/synthetix-java-fullstack")
                    .postedDate(LocalDate.now().minusDays(5))
                    .build(),

                Job.builder()
                    .title("Graduate Software Engineer")
                    .company("ApexGlobal IT")
                    .location("Kolkata, West Bengal (On-site)")
                    .jobType("FULL_TIME")
                    .experienceLevel("FRESHER")
                    .salaryMin(new BigDecimal("400000"))
                    .salaryMax(new BigDecimal("650000"))
                    .description("ApexGlobal is hiring talented CS/IT graduates for our software engineering cohort. You will receive structured training on Java enterprise frameworks, agile delivery, database design, and automated testing.")
                    .requiredSkills("Java, Core Java Concepts, DBMS/SQL, Git, Data Structures")
                    .preferredSkills("Spring Boot, Web Services, Linux")
                    .applicationUrl("https://example.com/careers/apexglobal-graduate-se")
                    .postedDate(LocalDate.now().minusDays(8))
                    .build()
            );

            jobRepository.saveAll(sampleJobs);
            log.info("Successfully seeded {} development job postings.", sampleJobs.size());
        } else {
            log.info("Jobs table already contains {} records. Skipping seeding.", jobRepository.count());
        }
    }
}
