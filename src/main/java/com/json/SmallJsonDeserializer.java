package com.json;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.json.domain.User;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;


@State(Scope.Thread)
public class SmallJsonDeserializer {

    private String json;

    private ObjectMapper mapper;

    private Gson gson;

    @Setup
    public void setup() {
        User user = new User();
        user.setId(1L);
        user.setName("赵侠客");
        user.setAge(29);
        user.setSex("男");
        user.setTrueName("公众号");
        json = JSON.toJSONString(user);
        mapper = new ObjectMapper();
        gson = new Gson();
    }

    @TearDown
    public void tearDown() {
        json = null;
        mapper = null;
        gson = null;
    }


    @Benchmark
    public void testFastJson() {
        User user = JSON.parseObject(json, User.class);
    }

    @Benchmark
    public void testFast2Json() {
        User user = com.alibaba.fastjson2.JSON.parseObject(json, User.class);
    }

    @Benchmark
    public void testHutoolJson() {
        User user = JSONUtil.toBean(json, User.class);
    }

    @Benchmark
    public void testJackson() throws JsonProcessingException {
        User user = mapper.readValue(json, User.class);
    }

    @Benchmark
    public void testGson() {
        User user = gson.fromJson(json, User.class);
    }


    @Test
    public void testBenchmark() throws Exception {
        Options options = new OptionsBuilder()
                .include(SmallJsonDeserializer.class.getSimpleName())
                .forks(1)
                .threads(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .mode(Mode.Throughput)
                .build();
        new Runner(options).run();
    }
}