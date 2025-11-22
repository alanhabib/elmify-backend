package com.elmify.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionCategoryId implements Serializable {

    @Column(name = "collection_id")
    private Long collectionId;

    @Column(name = "category_id")
    private Long categoryId;
}
