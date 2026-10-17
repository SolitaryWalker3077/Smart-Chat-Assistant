package com.ai.chat.bot.rag;

import com.alibaba.cloud.ai.advisor.RetrievalRerankAdvisor;
import com.alibaba.cloud.ai.model.RerankModel;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

public class AdvisorFactory {

    public static QuestionAnswerAdvisor createQuestionAnswerAdvisor(VectorStore vectorStore){
        PromptTemplate promptTemplate = new PromptTemplate("""
                【用户问题信息】
                {query}
                【上下文信息】
                {question_answer_context}
               
                回答请遵循以下规则：
                1. 若上下文信息中包含与用户问题直接相关的内容，即使表述方式略有不同，也应通过语义理解判断其相关性，并据此进行回复。
                2. 如果用户问题与课程无关, 请结合模型已有的知识进行回答.
                3. 如果用户咨询课程相关问题, 在上下文中找不到, 或者用户明确要求"人工客服","人工顾问'来回答, 请帮忙创建工单, 并让用户等待专业课程顾问回答.
                4. 回复时直接给出答案或解决方案，避免使用“根据您提供的信息”、“根据上下文”等冗余表达。
                确保系统具备基本的语义匹配能力，不能仅依赖字面完全匹配来判断信息相关性
              
                请参考以下对话示例的风格和方式回应客户:
                ---
                【用户】如果来不及听直播的话, 可以回放吗?
                【助手】当然可以！如果您无法按时参加直播课程，完全不用担心。\n 所有直播课程都会全程录制并生成回放视频，您可以在【图灵在线/在线OJ竞技场】学习平台上随时查看，支持反复观看、倍速播放等功能。无论因何原因错过直播，都不会影响您的学习进度.
                 """);
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .promptTemplate(promptTemplate)
                .build();
        return questionAnswerAdvisor;
    }

    public static Advisor createRerankAdvisor(VectorStore vectorStore, RerankModel rerankModel){
        RetrievalRerankAdvisor advisor = new RetrievalRerankAdvisor(vectorStore, rerankModel,
                SearchRequest.builder().topK(100).build());
        return advisor;
    }

}