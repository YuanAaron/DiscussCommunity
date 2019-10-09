package com.oshacker.discusscommunity.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;

public class WkImageDeleteJob implements Job {

    private static final Logger logger= LoggerFactory.getLogger(WkImageDeleteJob.class);

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        File[] files = new File(wkImageStorage).listFiles();
        if (files==null || files.length==0) {
            logger.info("没有WK图片，任务取消!");
            return;
        }

        //删除一分钟之前创建的图片
        for (File file: files) {
            if (System.currentTimeMillis()-file.lastModified()>60*1000) {
                logger.info("删除WK图片："+file.getName());
                file.delete();
            }
        }
    }
}
