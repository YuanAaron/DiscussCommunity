package com.oshacker.discusscommunity.dao;

import com.oshacker.discusscommunity.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated //不再推荐使用，转用Redis存储ticket
public interface LoginTicketMapper {

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired})"
    })
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    //这里其实不需要动态SQL，仅仅是为了展示动态SQL在注解方式中的写法
    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1=1 ",
            "</if>",
            "</script>"
    })
    int updateStatus(@Param("ticket") String ticket,
                     @Param("status") int status);

}
