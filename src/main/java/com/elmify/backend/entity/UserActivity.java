package com.elmify.backend.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
@Data
public class UserActivity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(name = "activity_type", nullable = false)
  private ActivityType activityType;

  @Column(name = "description")
  private String description;

  @Column(name = "ip_address")
  private String ipAddress;

  @Column(name = "user_agent")
  private String userAgent;

  @Column(name = "session_id")
  private String sessionId;

  @Column(name = "metadata", columnDefinition = "TEXT")
  private String metadata;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  // Static helper methods
  public static UserActivity createLoginActivity(User user, String ipAddress, String userAgent) {
    UserActivity activity = new UserActivity();
    activity.setUser(user);
    activity.setActivityType(ActivityType.LOGIN);
    activity.setDescription("User logged in");
    activity.setIpAddress(ipAddress);
    activity.setUserAgent(userAgent);
    return activity;
  }

  public static UserActivity createLecturePlayActivity(
      User user, Long lectureId, String lectureName) {
    UserActivity activity = new UserActivity();
    activity.setUser(user);
    activity.setActivityType(ActivityType.LECTURE_PLAY);
    activity.setDescription("Started playing: " + lectureName);
    activity.setMetadata(
        "{\"lectureId\":" + lectureId + ",\"lectureName\":\"" + lectureName + "\"}");
    return activity;
  }

  public static UserActivity createLectureCompleteActivity(
      User user, Long lectureId, String lectureName, Long durationListened) {
    UserActivity activity = new UserActivity();
    activity.setUser(user);
    activity.setActivityType(ActivityType.LECTURE_COMPLETE);
    activity.setDescription("Completed: " + lectureName);
    activity.setMetadata(
            "{\"lectureId\":\"" + lectureId + "\",\"lectureName\":\"" + lectureName + "\",\"durationListened\":\"" + durationListened + "\"}");
    return activity;
  }

  // Activity types enum
  public enum ActivityType {
    LOGIN("User Login"),
    LOGOUT("User Logout"),
    REGISTRATION("User Registration"),
    PASSWORD_CHANGE("Password Change"),
    EMAIL_VERIFICATION("Email Verification"),
    TWO_FACTOR_SETUP("Two Factor Setup"),

    LECTURE_PLAY("Lecture Play"),
    LECTURE_PAUSE("Lecture Pause"),
    LECTURE_COMPLETE("Lecture Complete"),
    LECTURE_UPLOAD("Lecture Upload"),
    LECTURE_DOWNLOAD("Lecture Download"),

    SEARCH("Search"),
    BROWSE_CATEGORY("Browse Category"),
    VIEW_SPEAKER("View Speaker"),
    VIEW_COLLECTION("View Collection"),

    PROFILE_UPDATE("Profile Update"),
    PREFERENCES_UPDATE("Preferences Update"),
    SUBSCRIPTION_CHANGE("Subscription Change"),
    ROLE_CHANGE("Role Change"),
    ACCOUNT_ACTIVATED("Account Activated"),
    ACCOUNT_DEACTIVATED("Account Deactivated"),

    USER_BANNED("User Banned"),
    USER_UNBANNED("User Unbanned"),
    CONTENT_MODERATION("Content Moderation"),

    API_ACCESS("API Access"),
    ERROR_OCCURRED("Error Occurred"),
    SYSTEM_MAINTENANCE("System Maintenance");

    private final String displayName;

    ActivityType(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }
}
