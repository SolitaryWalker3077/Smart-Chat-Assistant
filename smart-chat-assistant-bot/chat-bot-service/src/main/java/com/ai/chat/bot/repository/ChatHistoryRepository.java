package com.ai.chat.bot.repository;

import com.ai.chat.bot.entity.ChatInfo;

import java.util.List;

public interface ChatHistoryRepository {
    void save(String chatId, String title);
    void clearByChatId(String chatId);
    List<ChatInfo> getChats();
}
