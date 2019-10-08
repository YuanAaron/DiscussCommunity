package com.oshacker.discusscommunity.controller;

import com.oshacker.discusscommunity.entity.Event;
import com.oshacker.discusscommunity.event.EventProducer;
import com.oshacker.discusscommunity.utils.DiscussCommunityConstant;
import com.oshacker.discusscommunity.utils.DiscussCommunityUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements DiscussCommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(ShareController.class);

    @Value("${discusscommunity.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Autowired
    private EventProducer eventProducer;

    //获取长图:localhost:8080/community/share/image/f3a656b95744495899c2d2274e1b2df6
    @RequestMapping(path="/share/image/{fileName}",method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名不能为空!");
        }

        // 响应图片
        response.setContentType("image/png");
        fileName = wkImageStorage + "/" + fileName+".png"; //图片在服务器中的存放路径
        try (
                OutputStream os = response.getOutputStream();
                FileInputStream fis = new FileInputStream(fileName);
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("获取长图失败: " + e.getMessage());
        }
    }

    //生成长图:http://localhost:8080/community/share?htmlUrl=https://www.nowcoder.com
    @RequestMapping(path="/share",method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl) {
        //文件名
        String fileName= DiscussCommunityUtil.generateUUID();

        Event event=new Event().setTopic(TOPIC_SHARE)
                .setData("htmlUrl",htmlUrl)
                .setData("fileName",fileName)
                .setData("suffix",".png");
        eventProducer.fireEvent(event);

        Map<String,Object> map=new HashMap<>();
        map.put("shareUrl",domain+contextPath+"/share/image/"+fileName);
        return DiscussCommunityUtil.getJSONString(0,null,map);
    }
}
