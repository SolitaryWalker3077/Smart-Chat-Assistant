package com.ticket.demo;

import com.ticket.demo.entity.TicketInfo;
import com.ticket.demo.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Test
    void createTicket() {
        String result = ticketService.createTicket("测试", "测试测试", "chatId236");
        System.out.println(result);
    }

    @Test
    void queryTicketInfo() {
        TicketInfo ticketInfo = ticketService.queryTicketInfo("TL-SF-1792227420033-5440");
        System.out.println(ticketInfo);
    }
}
