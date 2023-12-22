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
    if (num <= 0) {
        System.out.println("库存不足了！");
        break;
    }
    num -= 1;
    redisComponent.set(CACHE_NUM_KEY, num);
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
```java
RLock lock = redissonClient.getLock(REDISSON_LOCK_KEY);
boolean tryLock = lock.tryLock();
if (!tryLock) {
    // 锁不可用
    continue;
}
try {
    int num = (int) redisComponent.get(CACHE_NUM_KEY);
    System.out.println("port: " + port + " >>> 当前num的值为：" + num);
    if (num <= 0) {
        System.out.println("库存不足了！");
        break;
    }
    num -= 1;
    redisComponent.set(CACHE_NUM_KEY, num);
    // 持有锁保持20秒，看门狗守护线程每10秒自动续期
    TimeUnit.SECONDS.sleep(20);
} catch (InterruptedException e) {
    throw new RuntimeException(e);
} finally {
    // 释放锁
    lock.unlock();
}
```
使用 redisson 的方式也能达到分布式锁的效果， 它默认设置持有锁的时间为30s
看门狗守护线程每隔10秒查看key是否存在，如果存在重置key的ttl

如果服务或者服务器宕机了，锁会在30秒内释放

问题：如果看门狗守护线程和redis之间出现了网络问题，然后业务的执行还未结束
而锁已经到期了，则会出现问题，其他服务节点就会获取到锁。

如果锁的持有时间太长，会导致服务意外宕机后，锁需要很长时间才能释放。

## 通过信号量实现秒杀

# 分布式锁和同步器

## 可重入锁（Reentrant Lock）

## 公平锁（Fair Lock）

## 联锁（Multi Lock）

## 红锁（Red Lock）

## 读写锁（Read Write Lock）

## 信号量（Semaphore）

## 可过期性信号量（Permit Expirable Semaphore）

## 闭锁（CountDownLatch）

# 源码摘录

## lua相关脚本

```lua
if (redis.call("exists", KEYS[1]) == 0) or (redis.call("hexists", KEYS[1], ARGV[2]) == 1) then
  redis.call("hincrby", KEYS[1], ARGV[2], 1)
  redis.call("pexpire", KEYS[1], ARGV[1])
  return nil
end
return redis.call("pttl", KEYS[1])
```
1、如果key不存在，或者对应线程id作为key存在，然后将 线程id作为key对应的value加1，并且设置过期时间
2、如果key存在，则返回剩余时间

```lua
if redis.call("hexists", KEYS[1], ARGV[2]) == 1 then
  redis.call("pexpire", KEYS[1], ARGV[1])
  return 1
end
return 0
```
如果某key对应hash存在指定线程id，则重置该key过期时间