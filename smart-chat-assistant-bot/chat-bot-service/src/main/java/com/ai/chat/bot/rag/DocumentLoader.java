package com.ai.chat.bot.rag;


import jdk.dynalink.linker.LinkerServices;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.renderer.markdown.MarkdownRenderer;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * 文档加载
 * */
@Slf4j
@Component
public class DocumentLoader {

    @Autowired
    private ResourceLoader resourceLoader;

    public Resource[] getResource(String location) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(resourceLoader);
        return resolver.getResources(location);
    }

    public List<Document> loadMarkdown() {
        List<Document> allDoc = new ArrayList<>();
        try {
            Resource[] resources = getResource("classpath:/file/*.md");
            //对每个Resource处理
            for (Resource resource : resources) {
                log.info("load file" + resource.getFilename());
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeBlockquote(false)
                        .withIncludeBlockquote(false)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource,config);
                allDoc.addAll(reader.get());
            }
        } catch (IOException e) {
            log.error("文档加载失败",e);
        }
        log.info("文档加载完成");
        return allDoc;
    }

}
