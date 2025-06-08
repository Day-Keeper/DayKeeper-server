package com.shujinko.project.domain.entity.diary;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class KeywordEmotion {
    Map<String,Long> emotions = new HashMap<>();
    Map<String,Long> keywords = new HashMap<>();
}
