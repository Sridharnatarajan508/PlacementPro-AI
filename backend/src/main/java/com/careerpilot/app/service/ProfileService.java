package com.careerpilot.app.service;

import com.careerpilot.app.dto.request.ProfileRequest;
import com.careerpilot.app.dto.response.ProfileResponse;
import com.careerpilot.app.entity.Profile;
import com.careerpilot.app.entity.User;
import com.careerpilot.app.exception.ResourceNotFoundException;
import com.careerpilot.app.repository.ProfileRepository;
import com.careerpilot.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileResponse getProfile(String email) {
        User user = findUserByEmail(email);
        Profile profile = profileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "userId", user.getId()));
        return toResponse(user, profile);
    }

    @Transactional
    public ProfileResponse updateProfile(String email, ProfileRequest request) {
        User user = findUserByEmail(email);
        Profile profile = profileRepository.findByUser(user)
                .orElse(Profile.builder().user(user).build());

        if (request.getCollege() != null) profile.setCollege(request.getCollege());
        if (request.getDegree() != null) profile.setDegree(request.getDegree());
        if (request.getBranch() != null) profile.setBranch(request.getBranch());
        if (request.getGraduationYear() != null) profile.setGraduationYear(request.getGraduationYear());
        if (request.getCgpa() != null) profile.setCgpa(request.getCgpa());
        if (request.getTargetRole() != null) profile.setTargetRole(request.getTargetRole());
        if (request.getPreferredJobType() != null) profile.setPreferredJobType(request.getPreferredJobType());
        if (request.getExperienceLevel() != null) profile.setExperienceLevel(request.getExperienceLevel());
        if (request.getSkillsSummary() != null) profile.setSkillsSummary(request.getSkillsSummary());
        if (request.getOnboardingComplete() != null) profile.setOnboardingComplete(request.getOnboardingComplete());

        profile = profileRepository.save(profile);
        log.info("Profile updated for user: {}", email);

        return toResponse(user, profile);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ProfileResponse toResponse(User user, Profile profile) {
        return ProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .college(profile.getCollege())
                .degree(profile.getDegree())
                .branch(profile.getBranch())
                .graduationYear(profile.getGraduationYear())
                .cgpa(profile.getCgpa())
                .targetRole(profile.getTargetRole())
                .preferredJobType(profile.getPreferredJobType())
                .experienceLevel(profile.getExperienceLevel())
                .skillsSummary(profile.getSkillsSummary())
                .onboardingComplete(profile.getOnboardingComplete())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
