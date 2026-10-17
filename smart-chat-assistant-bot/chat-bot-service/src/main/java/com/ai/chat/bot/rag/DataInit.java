package com.ai.chat.bot.rag;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class DataInit {

    @Autowired
    private DocumentLoader loader;

    @Autowired
    private KeywordEnricher enricher;

    @Autowired
    private VectorStore vectorStore;

    @Value("${data.is-load}")
    private boolean isLoad;  //true : 执行数据初始化   false : 不执行数据初始化

    //定义8个线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(8);


    @PostConstruct
    public void initData() {
        if(!isLoad){
            log.info("无需进行初始化");
            return;
        }

        //文档加载
        List<Document> documents = loader.loadMarkdown();
        log.info("文档加载完成,文档数:{}", documents.size());
        //文档分割
        CustomTokenTextSplitter splitter = new CustomTokenTextSplitter();
        List<Document> splitterDocuments = splitter.apply(documents);
        log.info("文档分割完成,文档数:{}", splitterDocuments.size());
//        //提取元信息
//        List<Document> enrichDocuments = enricher.enrich(splitterDocuments);
//        log.info("补充元信息,完成文档数:{}", enrichDocuments.size());
//        //向量的存储
//        vectorStore.add(enrichDocuments);
//        log.info("文档存储完成");
        processDocument(splitterDocuments,10);
        log.info("知识库搭建完成");
    }

    /**
     * 分批次处理文档
     *
     */
    private void processDocument(List<Document> documents, int batchSize) {
        log.info("开始处理文档, 文档个数:{}, 每批{}个", documents.size(), batchSize);
        int count = (int) Math.ceil((double) documents.size() / batchSize);

        List<List<Document>> batches = IntStream.range(0, count)
                .mapToObj(i -> new ArrayList<>(documents.subList(i * batchSize,
                        Math.min((i + 1) * batchSize, documents.size()))))
                .collect(Collectors.toList());
        log.info("共分为{}批次, 并发添加元数据, 写入向量数据库", batches.size());

        //针对每一个批次来进行处理
        CountDownLatch countDownLatch = new CountDownLatch(count);
        for (List<Document> part : batches) {
            executorService.submit(() -> {
                try {
                    //3. 提取元信息
                    List<Document> enrichDocuments = enricher.enrich(part);
                    log.info("批次补充元信息完成, 文档数:{}", enrichDocuments.size());
                    //4. 向量存储
                    vectorStore.add(enrichDocuments);
                    log.info("批次写入数据库完成, 文档数:{}", enrichDocuments.size());
                } catch (Exception e) {
                    log.error("批次处理失败, e:", e);
                } finally {
                    countDownLatch.countDown();  //不论任务成功失败, 都要减一
                }
            });
        }
        //等待所有任务执行完成
        try {
            countDownLatch.await(10, TimeUnit.MINUTES);
            log.info("所有批次都处理完成");
        } catch (InterruptedException e) {
            log.error("批次处理失败");
            throw new RuntimeException(e);
        }
    }
}

