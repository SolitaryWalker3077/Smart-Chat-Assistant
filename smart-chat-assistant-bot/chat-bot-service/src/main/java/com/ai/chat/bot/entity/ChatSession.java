package com.ai.chat.bot.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatSession {
    private Integer id;
    private String chatId;
    private String title;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
