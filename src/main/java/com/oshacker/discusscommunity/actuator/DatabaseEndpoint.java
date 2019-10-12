package com.oshacker.discusscommunity.actuator;

import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

//自定义端点监控数据库连接是否正常
@Component
@Endpoint(id="database")
public class DatabaseEndpoint {
    private static final Logger logger= LoggerFactory.getLogger(DatabaseEndpoint.class);

    @Autowired
    private DataSource dataSource;

    @ReadOperation //该端点只可以通过GET请求来访问
    public String checkConnection() {
        try (
                Connection conn = dataSource.getConnection();
        ) {
            return DiscussCommunityUtil.getJSONString(0,"获取连接成功!");
        } catch (SQLException e) {
            logger.error("获取连接失败："+e.getMessage());
            return DiscussCommunityUtil.getJSONString(1,"获取连接失败!");
        }


    }
}
