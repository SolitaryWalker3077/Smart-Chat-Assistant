package com.ticket.demo.entity;

import lombok.Data;

import java.util.Date;

@Data
public class TicketInfo {

    private Integer id;
    private String ticketId;
    private String title;
    private String description;
    private String relatedChatId;
    private Integer status;
    private String creator;
    private String assignee;
    private Date createdTime;
    private Date updatedTime;
    private Date closedTime;
}
