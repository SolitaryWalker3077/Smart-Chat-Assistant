package com.ai.chat.bot.rag;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 补充关键词元信息
 * */
@Slf4j
@Component
public class KeywordEnricher {

    @Autowired
    private DashScopeChatModel chatModel;

    /**
     * 补充关键词元信息
     * @param documents
     * @return
     */
    public List<Document> enrich(List<Document> documents) {
        KeywordMetadataEnricher enricher = KeywordMetadataEnricher
                .builder(chatModel)
                .keywordCount(5)
                .build();
        return enricher.apply(documents);
    }
}
