package com.json;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.json.domain.Article;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

/**
 * Benchmark： 整个基准测试的生命周期，多个线程共用同一份实例对象。该类内部的@Setup @TearDown注解的方法可能会被任一个线程执行，但是只会执行一次。
 * Group： 每一个Group内部共享同一个实例，需要配合@Group @GroupThread使用。该类内部的@Setup @TearDown注解的方法可能会该Group内的任一个线程执行，但是只会执行一次。
 * Thread：每个线程的实例都是不同的、唯一的。该类内部的@Setup @TearDown注解的方法只会被当前线程执行，而且只会执行一次。
 *
 */
@State(Scope.Thread)
public class BigJsonDerializer {

    private String json;
    private ObjectMapper mapper;
    private Gson gson;

    /**
     *  @Setup @TearDown
     * 在@Scope注解标示的类的方法上可以添加@Setup和@TearDwon注解。
     * @Setup：用来标示在Benchmark方法使用State对象之前需要执行的操作。
     * @TearDown：用来标示在Benchmark方法之后需要对State对象执行的操作。
     * @Setup、@TearDown支持设置Level级别，Level有三个值：
     *
     * Trial：      每次benchmark前/后执行一次，每次benchmark会包含多轮（Iteration）
     * Iteration：  每轮执行前/后执行一次
     * Invocation： 每次调用测试的方法前/后都执行一次，这个执行频率会很高，一般用不上。
     *
     * @throws IOException
     */
    @Setup(Level.Trial)
    public void setup() throws IOException {
        Article article = new Article();
        article.setId(10000L);
        article.setTenantId(10000L);
        article.setAuthor("公众号：赵侠客");
        article.setTitle(RandomUtil.randomString("主标题", 100));
        article.setSubTitle(RandomUtil.randomString("副标题", 50));
        article.setHtmlContent(new String(Files.readAllBytes(Paths.get("article.html"))));
        json = JSON.toJSONString(article);
        mapper = new ObjectMapper();
        gson = new Gson();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        json = null;
        mapper = null;
        gson = null;
    }


    @Benchmark
    public void testFastJson() {
        Article article = JSON.parseObject(json, Article.class);
    }

    @Benchmark
    public void testFast2Json() {
        Article article = com.alibaba.fastjson2.JSON.parseObject(json, Article.class);
    }


    @Benchmark
    public void testHutoolJson() {
        Article article = JSONUtil.toBean(json, Article.class);
    }

    @Benchmark
    public void testJackson() throws JsonProcessingException {
        Article article = mapper.readValue(json, Article.class);
    }

    @Benchmark
    public void testGson() {
        Article article = gson.fromJson(json, Article.class);
    }


    @Test
    public void testBenchmark() throws Exception {

        Options options = new OptionsBuilder()
                .include(BigJsonDerializer.class.getSimpleName())
                .forks(1)
                .threads(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .mode(Mode.Throughput)
                .build();
        new Runner(options).run();
    }
}