package com.shujinko.project.domain.entity.diary;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class DiaryEmotionId implements Serializable {
    
    private Long did;
    private Long eid;
    
    // equals() and hashCode() 꼭 구현
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiaryEmotionId)) return false;
        DiaryEmotionId that = (DiaryEmotionId) o;
        return Objects.equals(did, that.did) &&
                Objects.equals(eid, that.eid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(did, eid);
    }
}
