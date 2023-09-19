package com.atguigu.redis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 包名:com.atguigu.redis.controller
 *
 * @author Leevi
 * 日期2023-09-16  14:27
 */
@RestController
public class SkillController {
    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/skill1/{productId}")
    public String skill1(@PathVariable("productId") Integer productId){
        //1. 判断商品数量是否大于0
        Integer stock = (Integer) redisTemplate.opsForValue().get("skill:" + productId);
        if (stock > 0) {
            //2. 扣减库存
            redisTemplate.opsForValue().decrement("skill:" + productId);
            return "秒杀成功";
        }else {
            return "秒杀失败";
        }
    }

    @GetMapping("/skill2/{productId}")
    public String skill2(@PathVariable("productId") Integer productId){
        //1. 声明一个SessionCallback对象
        //SessionCallback: 会将多个命令放入到一个Redis的连接中，然后一次性发送给redis，减少网络开销
        SessionCallback<List> sessionCallback = new SessionCallback() {
            @Override
            public List execute(RedisOperations operations) throws DataAccessException {
                //1. watch
                operations.watch("skill:"+productId);
                //2. 判断库存
                Integer stock = (Integer) operations.opsForValue().get("skill:" + productId);
                if (stock > 0) {
                    //3. 开启事务
                    operations.multi();
                    //说明可以秒杀
                    //4. 扣减库存
                    operations.opsForValue().decrement("skill:" + productId);
                    return operations.exec();
                }
                return null;
            }
        };

        //2. 执行execute方法
        Object result = redisTemplate.execute(sessionCallback);

        if (result != null) {
            return "秒杀成功";
        }else {
            return "秒杀失败";
        }
    }

    @GetMapping("/skill3/{productId}")
    public String skill3(@PathVariable("productId") Integer productId){
        //1. 加载lua脚本
        RedisScript<Boolean> redisScript = RedisScript.of(new ClassPathResource("lua/skill.lua"),Boolean.class);

        //2. 执行lua脚本
        boolean result = (boolean) redisTemplate.execute(redisScript, Arrays.asList("skill:" + productId));

        return result ? "秒杀成功" : "秒杀失败";
    }
}
