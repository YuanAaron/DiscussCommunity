package com.oshacker.discusscommunity;

import com.oshacker.discusscommunity.utils.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest()
@ContextConfiguration(classes = DiscussCommunityApplication.class)
public class SensitiveWordsTests {

	@Autowired
	private SensitiveFilter sensitiveWordUtil;

	@Test
	public void testSensitiveFilter() {
		String text="fabc";
		System.out.println(sensitiveWordUtil.filter(text));
	}

}
