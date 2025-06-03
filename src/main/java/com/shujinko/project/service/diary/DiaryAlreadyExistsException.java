package com.shujinko.project.service.diary;

public class DiaryAlreadyExistsException extends RuntimeException{
    public DiaryAlreadyExistsException(String message){
        super(message);
    }
}
