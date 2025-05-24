package com.shujinko.project.domain.entity.diary;


import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@EqualsAndHashCode
@Getter
public class DiaryKeywordId implements Serializable {
    
    private Long did;
    private Long kid;
}
