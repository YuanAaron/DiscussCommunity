package com.oshacker.discusscommunity.service;

import com.oshacker.discusscommunity.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DataService {

    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat df=new SimpleDateFormat("yyyyMMdd");

    //将指定IP记入UV
    public void recordUV(String IP) {
        String uvKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(uvKey,IP);
    }

    //统计指定日期范围内的UV
    public long calculateUV(Date start,Date end) {
        if (start==null || end==null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        //获取指定日期范围内的key
        List<String> keyList=new ArrayList<>();
        //遍历日期
        Calendar st=Calendar.getInstance();
        Calendar en=Calendar.getInstance();
        st.setTime(start);
        en.setTime(end);
        while (!st.after(en)) {
            String key=RedisKeyUtil.getUVKey(df.format(st.getTime()));
            keyList.add(key);
            st.add(Calendar.DATE,1); //Calender.Date表示按天计，即向后移动一天
        }

        //合并这些数据
        String uvKey=RedisKeyUtil.getUVKey(df.format(start),df.format(end));
        redisTemplate.opsForHyperLogLog().union(uvKey, keyList.toArray());

        //返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(uvKey);
    }

    //将指定用户id记入DAU
    public void recordDAU(int userId) {
        String dauKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(dauKey,userId,true);
    }

    //统计指定日期范围内的DAU
    public long calculateDAU(Date start,Date end) {
        if (start==null || end==null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        //获取指定日期范围内的key
        List<byte[]> keyList=new ArrayList<>();
        //遍历日期
        Calendar st=Calendar.getInstance();
        Calendar en=Calendar.getInstance();
        st.setTime(start);
        en.setTime(end);
        while (!st.after(en)) {
            String key=RedisKeyUtil.getDAUKey(df.format(st.getTime()));
            keyList.add(key.getBytes());
            st.add(Calendar.DATE,1); //Calender.Date表示按天计，即向后移动一天
        }

        //进行OR运算：认为这段时间内只要访问过一次即为活跃用户（最简单）
        //进行AND运算：认为这段时间内每天都要访问过才为活跃用户
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String dauKey=RedisKeyUtil.getDAUKey(df.format(start),df.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        dauKey.getBytes(),  keyList.toArray(new byte[0][0]));
                return connection.bitCount(dauKey.getBytes());
            }
        });
    }
}
