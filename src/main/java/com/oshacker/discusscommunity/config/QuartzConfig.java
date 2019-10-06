package com.oshacker.discusscommunity.config;

import com.oshacker.discusscommunity.quartz.AlphaJob;
import com.oshacker.discusscommunity.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

@Configuration
public class QuartzConfig {
    //FactoryBean可简化Bean的实例化过程：
    //1、Spring通过FactoryBean封装了某些Bean的实例化过程(这里是JobDetail的实例化)；
    //2、将FactoryBean装配到Spring容器中；
    //3、将FactoryBean注入给其他的Bean;
    //4、该Bean("其他的Bean")得到的是FactoryBean所管理的对象实例

    //配置JobDetail
    //@Bean
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean=new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);
        factoryBean.setName("alphaJob");
        factoryBean.setGroup("alphaJobGroup");
        factoryBean.setDurability(true); //任务长久保存
        factoryBean.setRequestsRecovery(true); //任务是否可以被恢复
        return factoryBean;
    }

    //配置Trigger（CronTriggerFactoryBean,利用Cron表达式解决复杂的问题）
    //@Bean
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail) {
        SimpleTriggerFactoryBean factoryBean=new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail); //这个触发器对应哪个Job
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000); //3s
        factoryBean.setJobDataMap(new JobDataMap()); //用某个对象存储Job的状态
        return factoryBean;
    }

    //配置刷新帖子分数的任务
    @Bean
    public JobDetailFactoryBean PostScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean=new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("discussCommunityJobGroup");
        factoryBean.setDurability(true); //任务长久保存
        factoryBean.setRequestsRecovery(true); //任务是否可以被恢复
        return factoryBean;
    }

    //配置Trigger
    @Bean
    public SimpleTriggerFactoryBean PostScoreRefreshTrigger(JobDetail PostScoreRefreshJobDetail) {
        SimpleTriggerFactoryBean factoryBean=new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(PostScoreRefreshJobDetail); //这个触发器对应哪个Job
        factoryBean.setName("PostScoreRefreshTrigger");
        factoryBean.setGroup("discussCommunityTriggerGroup");
        factoryBean.setRepeatInterval(1000*60*5); //5min
        factoryBean.setJobDataMap(new JobDataMap()); //用某个对象存储Job的状态
        return factoryBean;
    }
}
