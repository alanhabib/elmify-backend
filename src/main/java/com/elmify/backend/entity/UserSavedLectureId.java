package com.elmify.backend.entity;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSavedLectureId implements Serializable {
    private Long user;    // Field name must match the entity's field name for the User
    private Long lecture; // Field name must match the entity's field name for the Lecture
}
