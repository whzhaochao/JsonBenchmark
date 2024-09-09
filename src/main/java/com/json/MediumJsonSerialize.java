package com.json;

import cn.hutool.core.util.RandomUtil;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

@State(Scope.Thread)
public class MediumJsonSerialize {

    private List<User> users;

    private ObjectMapper mapper;

    private Gson gson;

    @Setup
    public void setup() {
        users = new ArrayList<>();
        IntStream.range(1, 20).forEach(x -> {
            User user = new User();
            user.setId(1L);
            user.setName(RandomUtil.randomString("公众号:赵侠客",100));
            user.setAge(29);
            user.setSex("男");
            user.setTrueName(RandomUtil.randomString("公众号:赵侠客",100));
            user.setCreateTime(new Date());
            users.add(user);
        });
        mapper = new ObjectMapper();
        gson = new Gson();
    }

    @TearDown
    public void tearDown() {
        users = null;
        mapper = null;
        gson = null;
    }

    @Benchmark
    public void testFastJson() {
        String json = JSON.toJSONString(users);
    }

    @Benchmark
    public void testFast2Json() {
        String json = com.alibaba.fastjson2.JSON.toJSONString(users);
    }

    @Benchmark
    public void testHutoolJson() {
        String json = JSONUtil.toJsonStr(users);
    }


    @Benchmark
    public void testJackson() throws JsonProcessingException {
        String json = mapper.writeValueAsString(users);
    }

    @Benchmark
    public void testGson() {
        String json = gson.toJson(users);
    }


    @Test
    public void testBenchmark() throws Exception {
        Options options = new OptionsBuilder()
                .include(MediumJsonSerialize.class.getSimpleName())
                .forks(1)
                .threads(1)
                .warmupIterations(1)
                .measurementIterations(1)
                .mode(Mode.Throughput)
                .build();
        new Runner(options).run();
    }
}