package com.ai.chat.bot.mapper;

import com.ai.chat.bot.entity.ChatSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    @Insert("""
            INSERT INTO chat_sessions (chat_id, title)
            VALUES (#{chatId}, #{title})
            ON DUPLICATE KEY UPDATE title = #{title}, updated_time = CURRENT_TIMESTAMP
            """)
    int save(ChatSession session);

    @Delete("DELETE FROM chat_sessions WHERE chat_id = #{chatId}")
    int deleteByChatId(@Param("chatId") String chatId);

    @Select("""
            SELECT id, chat_id AS chatId, title,
                   created_time AS createdTime, updated_time AS updatedTime
            FROM chat_sessions
            ORDER BY id ASC
            """)
    List<ChatSession> findAll();
}
