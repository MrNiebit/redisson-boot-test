package cn.lacknb.redissonboottest.controller;

import cn.lacknb.redissonboottest.component.RedisComponent;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author gitsilence
 * @date 2023-07-21
 */
@RequestMapping("/test")
@RestController
public class TestController {

    @Autowired
    private RedisComponent redisComponent;

    private static final String CACHE_KEY = "redisson:redis-test";

    private static final String CACHE_NUM_KEY = "redisson:num-test";

    private static final String NUM_LOCK_KEY = "redisson:num-LOCK";

    @Value("${server.port}")
    private int port;

    @Autowired
    private RedissonClient redissonClient;

    @RequestMapping("/hello")
    public String hello() {
        return "hello spring boot !!!";
    }

    @RequestMapping("/redis")
    public Object redis() {
        Object o = redisComponent.get(CACHE_KEY);
        if (o != null) {
            return o;
        }
        String uuid = UUID.randomUUID().toString();
        System.out.println("生产 uuid: " + uuid);
        redisComponent.setex(CACHE_KEY, uuid, 20);
        return uuid;
    }

    @RequestMapping("/init")
    public String init() {
        redisComponent.set(CACHE_NUM_KEY, 300);
        return "初始化成功";
    }


    @RequestMapping("/buy")
    public String buy() {
        for (;;) {
            String threadId = UUID.randomUUID().toString();
            try {
                Boolean lock = redisComponent.setnx(NUM_LOCK_KEY, threadId);
                if (!lock) {
                    // System.out.println("未获取到锁, pass");
                    continue;
                }
                int num = (int) redisComponent.get(CACHE_NUM_KEY);
                System.out.println("port: " + port + " >>> 当前num的值为：" + num);
                num -= 1;
                redisComponent.set(CACHE_NUM_KEY, num);
                if (num <= 0) {
                    System.out.println("库存不足了！");
                    break;
                }
                TimeUnit.MICROSECONDS.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                String th = (String) redisComponent.get(NUM_LOCK_KEY);
                // 匹配的线程，才能释放锁
                if (threadId.equals(th)) {
                    redisComponent.del(NUM_LOCK_KEY);
                }
            }
        }
        return "over";
    }
}
