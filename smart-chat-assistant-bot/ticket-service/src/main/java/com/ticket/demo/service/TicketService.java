package com.ticket.demo.service;

import com.ticket.demo.entity.TicketInfo;
import com.ticket.demo.mapper.TicketMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class TicketService {
    @Autowired
    private TicketMapper ticketMapper;
    /**
     * 创建工单
     * @return
     */
    @Tool(description = "根据工单标题, 描述和关联会话ID, 创建工单")
    public String createTicket(@ToolParam(description = "工单标题, 不能为空") String title,
                               @ToolParam(description = "工单描述, 不能为空") String description,
                               @ToolParam(description = "关联的会话ID, 不能为空") String relatedChatId){
        log.info("创建工单, title:{}, description:{}, relatedChatId:{}", title, description, relatedChatId);
        //参数校验
        if (!StringUtils.hasText(title)){
            return "创建工单失败, 缺少必填参数'title'";
        }
        if (!StringUtils.hasText(description)){
            return "创建工单失败, 缺少必填参数'description'";
        }
        if (!StringUtils.hasText(relatedChatId)){
            return "创建工单失败, 缺少必填参数'relatedChatId'";
        }
        TicketInfo ticketInfo = parseTicketInfo(title, description, relatedChatId);

        try {
            Integer i = ticketMapper.insertTicket(ticketInfo);
            return String.format("工单创建成功, 工单ID:%s",ticketInfo.getTicketId());
        }catch (Exception e){
            return "工单创建失败, e:"+e.getMessage();
        }
    }

    private TicketInfo parseTicketInfo(String title, String description, String relatedChatId) {
        TicketInfo ticketInfo = new TicketInfo();
        ticketInfo.setTicketId(generateTicketId());
        ticketInfo.setTitle(title);
        ticketInfo.setDescription(description);
        ticketInfo.setRelatedChatId(relatedChatId);
        return ticketInfo;
    }

    /**
     * 生成工单ID: TL-SF-时间戳-4位随机数
     * (同一个毫秒下, 有多个线程时, 可能会重复, 概率不高, 生产环境不推荐使用)
     * @return
     */
    private String generateTicketId() {
        return String.format("TL-SF-%d-%04d", System.currentTimeMillis(),
                ThreadLocalRandom.current().nextInt(1000, 10000));
    }

    @Tool(description = "根据工单ID查询工单信息")
    public TicketInfo queryTicketInfo(@ToolParam(description = "工单ID, 不能为空") String ticketId){
        log.info("查询工单, ticketID:", ticketId);
        if (!StringUtils.hasText(ticketId)){
            return null;
        }
        TicketInfo ticketInfo = ticketMapper.selectTicketById(ticketId);
        return ticketInfo;
    }
}
