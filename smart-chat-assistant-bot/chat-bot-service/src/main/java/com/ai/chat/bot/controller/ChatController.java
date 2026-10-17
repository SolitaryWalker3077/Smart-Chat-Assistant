package com.ai.chat.bot.controller;

import com.ai.chat.bot.entity.ChatInfo;
import com.ai.chat.bot.entity.MessageVO;
import com.ai.chat.bot.repository.ChatHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;
    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;


    public ChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @RequestMapping(value = "/stream", produces = "text/html;charset=utf-8")
    public Flux<String> stream(String prompt, String chatId){
        log.info("chatId:{}, prompt:{}", chatId, prompt);
        chatHistoryRepository.save(chatId, prompt);
        return this.chatClient.prompt()
                .user(prompt)
                .system(build-> build.text("当前会话id: %s".formatted(chatId)))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .stream()
                .content();
    }

    /**
     * 获取会话列表
     * @return
     */
    @RequestMapping("/getChatIds")
    public List<ChatInfo> getChatIds(){
        return chatHistoryRepository.getChats();
    }

    /**
     * 获取会话记录
     * @param chatId
     */
    @RequestMapping("/getChatHistory")
    public List<MessageVO> getChatHistory(String chatId){
        log.info("获取会话记录, chatId:{}", chatId);
        List<Message> messages = chatMemory.get(chatId);
        return messages.stream().map(MessageVO::new).collect(Collectors.toList());
    }

    /**
     * 删除会话
     * @param chatId
     * @return
     */
    @RequestMapping("/deleteChat")
    public Boolean deleteChat(String chatId){
        log.info("删除会话, chatId:{}", chatId);
        try {
            chatHistoryRepository.clearByChatId(chatId);
            chatMemory.clear(chatId);
        }catch (Exception e){
            log.error("删除会话失败, chatId:{}", chatId);
            return false;
        }
        return true;
    }

}
