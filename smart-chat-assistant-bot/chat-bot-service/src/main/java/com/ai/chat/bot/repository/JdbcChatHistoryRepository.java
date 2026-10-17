package com.ai.chat.bot.repository;

import com.ai.chat.bot.entity.ChatInfo;
import com.ai.chat.bot.entity.ChatSession;
import com.ai.chat.bot.mapper.ChatSessionMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class JdbcChatHistoryRepository implements ChatHistoryRepository {

    private final ChatSessionMapper chatSessionMapper;

    public JdbcChatHistoryRepository(ChatSessionMapper chatSessionMapper) {
        this.chatSessionMapper = chatSessionMapper;
    }

    @Override
    public void save(String chatId, String title) {
        ChatSession session = new ChatSession();
        session.setChatId(chatId);
        // Keep the existing display title rules and fit the title column.
        session.setTitle(new ChatInfo(chatId, title).getTitle());
        chatSessionMapper.save(session);
    }

    @Override
    public void clearByChatId(String chatId) {
        chatSessionMapper.deleteByChatId(chatId);
    }

    @Override
    public List<ChatInfo> getChats() {
        return chatSessionMapper.findAll().stream()
                .map(session -> new ChatInfo(session.getChatId(), session.getTitle()))
                .toList();
    }
}
