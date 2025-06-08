package com.shujinko.project.repository.diary;

import com.shujinko.project.domain.entity.diary.Paragraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParagraphRepository extends JpaRepository<Paragraph,Long> {
}
