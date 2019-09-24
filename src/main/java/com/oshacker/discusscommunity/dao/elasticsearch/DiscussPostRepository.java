package com.oshacker.discusscommunity.dao.elasticsearch;

import com.oshacker.discusscommunity.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
//ElasticsearchRepository<DiscussPost,Integer>第一个参数：实体类，第二个参数：实体类中@id属性的类型
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost,Integer> {
}
