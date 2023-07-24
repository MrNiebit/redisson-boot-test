package cn.lacknb.redissonboottest.aspect;

import cn.lacknb.redissonboottest.annotation.DistributeKey;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Arrays;

/**
 * <h2>  </h2>
 *
 * @description:
 * @menu
 * @author: nbh
 * @description:
 * @date: 2023/7/24 9:58
 **/
@Component
@Aspect
public class DistributedLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    @Value("${server.port}")
    private String serverPort;

    private static final String COMMON_PREFIX = "redisson-lock";

    @Around("@annotation(distributeKey)")
    public Object lock(ProceedingJoinPoint point, DistributeKey distributeKey) throws Throwable {
        String value = distributeKey.value();
        boolean single = distributeKey.s();
        if (!single) {
            // 集群环境下，多服务并发执行
            // 获取当前机器IP和端口
            InetAddress address = InetAddress.getLocalHost();
            String ip = address.getHostAddress();
            value = String.join(":", Arrays.asList(COMMON_PREFIX,
                    ip + "-" + serverPort, value));
        }
        RLock lock = redissonClient.getLock(value);
        if (!lock.tryLock()) {
            return "未获取到锁";
        }
        try {
            return point.proceed();
        } finally {
            lock.unlock();
        }
    }

}
