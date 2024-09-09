package com.json;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.json.domain.User;
import org.junit.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@State(Scope.Thread)
public class MediumJsonDerializer {

    private String json;

    private ObjectMapper mapper;

    private Gson gson;

    @Setup
    public void setup() {
        List<User> users = new ArrayList<>();
        IntStream.range(1, 20).forEach(x -> {
            User user = new User();
            user.setId(1L);
            user.setName("赵侠客");
            user.setAge(29);
            user.setSex("男");
            user.setTrueName("公众号");
            users.add(user);
        });
        json = JSON.toJSONString(users);
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
        List<User> users = JSON.parseArray(json, User.class);
    }

    @Benchmark
    public void testFast2Json() {
        List<User> user = com.alibaba.fastjson2.JSON.parseArray(json, User.class);
    }


    @Benchmark
    public void testHutoolJson() {
        List<User> users = JSONUtil.toList(json, User.class);
    }

    @Benchmark
    public void testJackson() throws JsonProcessingException {
        JavaType javaType = mapper.getTypeFactory().constructCollectionType(List.class, User.class);
        List<User> users = mapper.readValue(json, javaType);
    }

    @Benchmark
    public void testGson() {
        Type type = new TypeToken<List<User>>() {
        }.getType();
        List<User> users = gson.fromJson(json, type);
    }


    @Test
    public void testBenchmark() throws Exception {
        Options options = new OptionsBuilder()
                .include(MediumJsonDerializer.class.getSimpleName())
                .forks(1)
                .threads(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .mode(Mode.Throughput)
                .build();
        new Runner(options).run();
    }
}