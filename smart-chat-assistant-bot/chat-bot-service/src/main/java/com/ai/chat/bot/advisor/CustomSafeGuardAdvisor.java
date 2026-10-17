package com.ai.chat.bot.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public class CustomSafeGuardAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String DEFAULT_FAILURE_RESPONSE = "由于内容敏感, 我无法回复, 我们能讨论点别的吗?";
    private static final int DEFAULT_ORDER = 0;

    private final String failureResponse;

    private final List<String> sensitiveWords;

    private final int order;

    public CustomSafeGuardAdvisor(List<String> sensitiveWords) {
        this(sensitiveWords, DEFAULT_FAILURE_RESPONSE, DEFAULT_ORDER);
    }

    public CustomSafeGuardAdvisor(List<String> sensitiveWords, String failureResponse, int order) {
        Assert.notNull(sensitiveWords, "Sensitive words must not be null!");
        Assert.notNull(failureResponse, "Failure response must not be null!");
        this.sensitiveWords = sensitiveWords;
        this.failureResponse = failureResponse;
        this.order = order;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        if (!CollectionUtils.isEmpty(this.sensitiveWords)
                && this.sensitiveWords.stream().anyMatch(w ->
                chatClientRequest.prompt().getUserMessage().getText().contains(w))) {
            return createFailureResponse(chatClientRequest);
        }

        return callAdvisorChain.nextCall(chatClientRequest);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest,
                                                 StreamAdvisorChain streamAdvisorChain) {
        if (!CollectionUtils.isEmpty(this.sensitiveWords)
                && this.sensitiveWords.stream().anyMatch(w ->
                chatClientRequest.prompt().getUserMessage().getText().contains(w))) {
            return Flux.just(createFailureResponse(chatClientRequest));
        }

        return streamAdvisorChain.nextStream(chatClientRequest);
    }

    private ChatClientResponse createFailureResponse(ChatClientRequest chatClientRequest) {
        return ChatClientResponse.builder()
                .chatResponse(ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage(this.failureResponse))))
                        .build())
                .context(Map.copyOf(chatClientRequest.context()))
                .build();
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    public static final class Builder {

        private List<String> sensitiveWords;

        private String failureResponse = DEFAULT_FAILURE_RESPONSE;

        private int order = DEFAULT_ORDER;

        private Builder() {
        }

        public Builder sensitiveWords(List<String> sensitiveWords) {
            this.sensitiveWords = sensitiveWords;
            return this;
        }

        public Builder failureResponse(String failureResponse) {
            this.failureResponse = failureResponse;
            return this;
        }

        public Builder order(int order) {
            this.order = order;
            return this;
        }

        public CustomSafeGuardAdvisor build() {
            return new CustomSafeGuardAdvisor(this.sensitiveWords, this.failureResponse, this.order);
        }

    }

}
