package com.audibleclone.backend.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDto {
    
    // Playback preferences
    private Boolean autoplay = true;
    private Double playbackSpeed = 1.0;
    private Integer skipForwardSeconds = 30;
    private Integer skipBackwardSeconds = 15;
    
    // Audio preferences
    private Double volume = 1.0;
    private Boolean enhancedAudio = false;
    private String audioQuality = "HIGH"; // LOW, MEDIUM, HIGH
    
    // Content preferences
    private String preferredLanguage = "en";
    private Boolean showExplicitContent = false;
    private Boolean autoDownload = false;
    private Integer downloadQuality = 128; // kbps
    
    // Privacy preferences
    private Boolean shareListeningHistory = false;
    private Boolean allowAnalytics = true;
    private Boolean receiveRecommendations = true;
    
    // Notification preferences
    private Boolean emailNotifications = true;
    private Boolean pushNotifications = true;
    private Boolean newContentNotifications = true;
    private Boolean subscriptionNotifications = true;
    
    // Display preferences
    private String theme = "LIGHT"; // LIGHT, DARK, AUTO
    private String dateFormat = "MM/dd/yyyy";
    private String timeFormat = "12h"; // 12h, 24h
    private String timezone = "UTC";

    // Daily goals
    private Integer dailyGoalMinutes = 20; // Default daily listening goal in minutes

    // Accessibility preferences
    private Boolean highContrast = false;
    private Boolean largeText = false;
    private Boolean reduceMotion = false;
    private Boolean screenReader = false;
}