package com.ai.chat.bot.repository;

import com.ai.chat.bot.entity.ChatInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MemoryChatHistoryRepository implements ChatHistoryRepository{

    private Map<String, String> chatInfos = new LinkedHashMap<>();

    /**
     * 保存会话记录
     * 如果是新会话, 则新增
     * 如果会话已经存在, 则更新
     */
    @Override
    public void save(String chatId, String title) {
        chatInfos.put(chatId, title);
    }

    @Override
    public void clearByChatId(String chatId) {
        chatInfos.remove(chatId);
    }

    @Override
    public List<ChatInfo> getChats() {
        return chatInfos.entrySet().stream()
                .map(enrty-> new ChatInfo(enrty.getKey(), enrty.getValue()))
                .collect(Collectors.toList());
    }
}
