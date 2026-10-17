package com.ai.chat.bot.config;

import com.ai.chat.bot.advisor.CustomSafeGuardAdvisor;
import com.ai.chat.bot.rag.AdvisorFactory;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.model.RerankModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CommonConfiguration {
//    @Bean
//    public ChatMemory chatMemory(){
//        return MessageWindowChatMemory.builder()
//                .maxMessages(10)
//                .build();
//    }

    public ChatMemory chatMemory(JdbcChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10) //每个会话最多保留的消息个数
                .build();
    }

    @Bean
    public ChatClient dashScopeChatClient(DashScopeChatModel chatModel,
                                          ChatMemory chatMemory,
                                          VectorStore vectorStore,
                                          RerankModel rerankModel,
                                          ToolCallbackProvider toolCallbackProvider){
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一名专业的企业培训课程咨询助手, 代表【图灵算法教育】为客户提供课程咨询服务.
                        你的职责是准确、礼貌、高效地解答客户关于企业提供的培训课程的各类问题
                """)
                .defaultAdvisors(new SimpleLoggerAdvisor(), MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultAdvisors(AdvisorFactory.createQuestionAnswerAdvisor(vectorStore))
                .defaultAdvisors(AdvisorFactory.createRerankAdvisor(vectorStore,rerankModel))
                .defaultAdvisors(new CustomSafeGuardAdvisor(List.of("公务员", "政府")))
                .defaultToolCallbacks(toolCallbackProvider)
                .build();
    }
}
