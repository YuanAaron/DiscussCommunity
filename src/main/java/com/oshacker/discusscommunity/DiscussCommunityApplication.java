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

	public static void main(String[] args) {
		SpringApplication.run(DiscussCommunityApplication.class, args);
	}

}
