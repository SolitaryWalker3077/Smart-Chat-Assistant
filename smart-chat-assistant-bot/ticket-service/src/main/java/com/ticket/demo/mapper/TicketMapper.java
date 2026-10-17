package com.ticket.demo.mapper;

import com.ticket.demo.entity.TicketInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TicketMapper {

    @Insert("""
        INSERT INTO ticket_info
        (ticket_id, title, `description`, related_chat_id)
        VALUES
        (#{ticketId}, #{title}, #{description}, #{relatedChatId})
        """)
    @Options(
            useGeneratedKeys = true,
            keyProperty = "id",
            keyColumn = "id"
    )
    int insertTicket(TicketInfo ticketInfo);


    @Select("select * from ticket_info where ticket_id = #{ticketId}")
    TicketInfo selectTicketById(String ticketId);
}
