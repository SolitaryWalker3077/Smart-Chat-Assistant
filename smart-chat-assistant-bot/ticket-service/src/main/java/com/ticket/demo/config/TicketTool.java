package com.ticket.demo.config;

import com.ticket.demo.service.TicketService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TicketTool {
    @Bean
    public ToolCallbackProvider toolCallbackProvider(TicketService ticketService){
        return MethodToolCallbackProvider.builder()
                .toolObjects(ticketService)
                .build();
    }
}
