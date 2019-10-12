package com.oshacker.discusscommunity;

import com.oshacker.discusscommunity.entity.DiscussPost;
import com.oshacker.discusscommunity.service.DiscussPostService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = DiscussCommunityApplication.class)
public class SpringBootTests {

    @Autowired
    private DiscussPostService postService;

    private DiscussPost discussPost;

    @BeforeClass
    public static void beforeClass() {
        System.out.println("beforeClass");
    }

    @AfterClass
    public static void afterClass() {
        System.out.println("afterClass");
    }

    @Before
    public void before() {
        System.out.println("before");

        discussPost=new DiscussPost();
        discussPost.setUserId(111);
        discussPost.setTitle("Test Title");
        discussPost.setContent("Test Content");
        discussPost.setCreateTime(new Date());
        postService.addDiscussPost(discussPost);

    }

    @After
    public void after() {
        System.out.println("after");

        postService.updateStatus(discussPost.getId(),2);
    }

    @Test
    public void test1() {
        System.out.println("test1");
    }

    @Test
    public void test2() {
        System.out.println("test2");
    }

    @Test
    public void testFindById() {
        DiscussPost post=postService.findDiscussPostById(discussPost.getId());
        Assert.assertNotNull(post);
        Assert.assertEquals(discussPost.getTitle(),post.getTitle());
        Assert.assertEquals(discussPost.getContent(),post.getContent());
    }

    @Test
    public void testUpdateScore() {
        int rows=postService.updateScore(discussPost.getId(),2000);
        Assert.assertEquals(1,rows);

        DiscussPost post = postService.findDiscussPostById(discussPost.getId());
        Assert.assertEquals(2000.00,post.getScore(),2);
    }
}
