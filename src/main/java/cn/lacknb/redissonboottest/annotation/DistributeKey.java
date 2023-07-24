package cn.lacknb.redissonboottest.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h2>  </h2>
 *
 * @description:
 * @menu
 * @author: nbh
 * @description:
 * @date: 2023/7/24 10:02
 **/
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributeKey {

    /**
     * cache key
     * @return
     */
    String value();

    /**
     * 锁的粒度，
     * true  加 IP+端口作为前缀
     * false 不加
     * @return
     */
    boolean single() default true;
}
