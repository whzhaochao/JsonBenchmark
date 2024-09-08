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

@State(Scope.Thread)
public class BigJsonSerialize {

    private Article article;

    private ObjectMapper mapper;

    private Gson gson;

    @Setup
    public void setup() throws IOException {
        article = new Article();
        article.setId(10000L);
        article.setTenantId(10000L);
        article.setAuthor("公众号：赵侠客");
        article.setPublishTime(new Date());
        article.setTitle(RandomUtil.randomString("主标题", 100));
        article.setSubTitle(RandomUtil.randomString("副标题", 50));
        article.setHtmlContent(new String(Files.readAllBytes(Paths.get("/Users/zhaochao/IdeaProjects/DownloadDemo/article.html"))));
        mapper = new ObjectMapper();
        gson = new Gson();
    }

    @TearDown
    public void tearDown() {
        article = null;
        mapper = null;
        gson = null;
    }


    @Benchmark
    public void testFastJson() {
        String json = JSON.toJSONString(article);
    }

    @Benchmark
    public void testFast2Json() {
        String json = com.alibaba.fastjson2.JSON.toJSONString(article);
    }


    @Benchmark
    public void testHutoolJson() {
        String json = JSONUtil.toJsonStr(article);
    }

    @Benchmark
    public void testJackson() throws JsonProcessingException {
        String json = mapper.writeValueAsString(article);
    }

    @Benchmark
    public void testGson() {
        String json = gson.toJson(article);
    }


    @Test
    public void testBenchmark() throws Exception {
        Options options = new OptionsBuilder()
                .include(BigJsonSerialize.class.getSimpleName())
                .forks(1)
                .threads(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .mode(Mode.Throughput)
                .build();
        new Runner(options).run();
    }
}