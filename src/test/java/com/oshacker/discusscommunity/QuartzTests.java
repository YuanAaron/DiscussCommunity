package com.oshacker.discusscommunity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DiscussCommunityApplication.class)
public class QuartzTests {

    @Autowired
    private Scheduler scheduler;

    //删除数据库Quartz相关表中的Job数据,
    //否则启动程序就会读取相关表中的数据来执行定时任务
    @Test
    public void testDeleteJob() {
        boolean res = false;
        try {
            res = scheduler.deleteJob(new JobKey("alphaJob", "alphaJobGroup"));
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        System.out.println(res);

    }
}
