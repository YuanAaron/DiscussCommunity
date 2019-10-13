package com.oshacker.discusscommunity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class DiscussCommunityApplication {

	@PostConstruct
	public void init() {
		//解决Netty启动冲突的问题(看Netty4Utils.setAvailableProcessors()方法)
		System.setProperty("es.set.netty.runtime.available.processors","false");
	}

	//当前程序部署在tomcat中，tomcat是Java程序，它本身就有main方法，而一个Java程序不可能有两个main方法，
	//不可能main调用main,因此需要创建tomcat调用本项目程序的入口，见DiscussCommunityServletInitializer。
	public static void main(String[] args) {
		SpringApplication.run(DiscussCommunityApplication.class, args);
	}

}
