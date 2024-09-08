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

import java.util.Date;

@State(Scope.Thread)
public class SmallJsonSerialize {

    private User user;
    private ObjectMapper mapper;
    private Gson gson;

    @Setup
    public void setup() {
        user = new User();
        user.setId(1L);
        user.setName("赵侠客");
        user.setAge(29);
        user.setSex("男");
        user.setTrueName("公众号");
        user.setCreateTime(new Date());
        mapper = new ObjectMapper();
        gson = new Gson();
    }

    @TearDown
    public void tearDown() {
        user = null;
        mapper = null;
        gson = null;
    }

    @Benchmark
    public void testFastJson() {
        String json = JSON.toJSONString(user);
    }

    @Benchmark
    public void testFast2Json() {
        String json = com.alibaba.fastjson2.JSON.toJSONString(user);
    }

    @Benchmark
    public void testHutoolJson() {
        String json = JSONUtil.toJsonStr(user);
    }

    @Benchmark
    public void testJackson() throws JsonProcessingException {
        String json = mapper.writeValueAsString(user);
    }

    @Benchmark
    public void testGson() {
        String json = gson.toJson(user);
    }


    @Test
    public void testBenchmark() throws Exception {
        Options options = new OptionsBuilder()
                .include(SmallJsonSerialize.class.getSimpleName())
                .forks(1)
                .threads(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .mode(Mode.Throughput)
                .build();
        new Runner(options).run();
    }
}