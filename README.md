# Redisson 各种场景下的使用

## 实现分布式锁

1、实现分布式方式有很多，例如 redis 的 setNx，如果设置成功返回true
否则返回0

接口实现：/test/buy

```java
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
} finally {
    String th = (String) redisComponent.get(NUM_LOCK_KEY);
    // 匹配的线程，才能释放锁
    if (threadId.equals(th)) {
        redisComponent.del(NUM_LOCK_KEY);
    }
}
```
两个服务打印结果：
```text
port: 8081 >>> 当前num的值为：300
port: 8081 >>> 当前num的值为：299
port: 8081 >>> 当前num的值为：297
port: 8081 >>> 当前num的值为：296
port: 8081 >>> 当前num的值为：295
port: 8081 >>> 当前num的值为：293
port: 8081 >>> 当前num的值为：292
port: 8081 >>> 当前num的值为：290

port: 8082 >>> 当前num的值为：298
port: 8082 >>> 当前num的值为：294
port: 8082 >>> 当前num的值为：291
port: 8082 >>> 当前num的值为：288
port: 8082 >>> 当前num的值为：283
port: 8082 >>> 当前num的值为：281
port: 8082 >>> 当前num的值为：277
port: 8082 >>> 当前num的值为：274
port: 8082 >>> 当前num的值为：273
```
通过这种方式实现分布式锁，如果服务器宕机，服务意外中止，都可能会导致
死锁的问题。

2、redisson 的方式


## 通过信号量实现秒杀

## redLock的概念
