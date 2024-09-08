>首发公众号:[【赵侠客】](https://mp.weixin.qq.com/s/QTyVFWUbKsR80s-DUj6vSA)
# 引言

在前面《[释放你九成的带宽和内存：GZIP在解决Redis大Key方面的应用](https://mp.weixin.qq.com/s/SydFf5KmcqOrYIX7gTzB2g)》一文中我使用GZIP算法对JSON格式数据进行了压缩，可以减小88%的空间，文中也提到了目前JSON格式在我们项目中应用的非常广泛几乎无处不在。压缩JSON格式数据可以大大降低我们的存储和带宽成本，我们知道数据格式的转换过程是需要消耗CPU计算资源的，JSON格式数据转换的快慢也会直接影响我们接口响应的快慢，甚至影响我们系统的吞吐量，所以本文针对市面上主流的5种JSON解析工具FastJson、FastJson2、JackSon、Gson、Hutool-JSON使用JAVA基准测试分别对小JSON、中JSON、大JSON的序列化和返序列化共6项指标进行测试，最后给出了测试排行榜，希望最后的排行榜能对您在做JSON工具选型时有一定的帮助。


# 准备工作

## JMH基准测试

平时我们做代码性能测试可能就是在代码执行前通过`System.currentTimeMillis()`获取一下当前时间，代码执行后再获取一个当前时间，然后两个时间相减得出代码的运行时间，这种测试是非常不准确的，包括获取时间的精度、JIT编译优化导致性能测试结果不稳定、系统当前的负载，包括CPU、内存、磁盘I/O等会影响测试结果、Java虚拟机（JVM）需要一段时间来预热也会影响测试结果。所以为了测试的准确性，本文使用`JMH(Java Microbenchmark Harness)`进行测试，`JMH`是由`OpenJDK/Oracle`维护的Java基准测试工具，它旨在帮助开发人员编写准确的基准测试，以避免常见的基准测试陷阱，并提供可靠的性能测试结果。因为使用的`JMH`基准测试所以测试结果应该是有说服力的。

添加`JMH Maven`依赖：

```java
<dependency>
<groupId>org.openjdk.jmh</groupId>
<artifactId>jmh-core</artifactId>
<version>1.36</version>
</dependency>
<dependency>
<groupId>org.openjdk.jmh</groupId>
<artifactId>jmh-generator-annprocess</artifactId>
<version>1.36</version>
</dependency>
```
JMH测试代码：
```java
@State(Scope.Thread)
public class HelloBenchmark {
    @Benchmark
    public void testMethod() throws InterruptedException {
        Thread.sleep(10);
    }
    @Test
    public void testBenchmark() throws Exception {
        Options options = new OptionsBuilder()
                .include(HelloBenchmark.class.getSimpleName())
                .forks(1) //进程数
                .threads(1) //线程数
                .warmupIterations(1)
                .measurementIterations(1)
                .mode(Mode.Throughput)
                .build();
        new Runner(options).run();
    }
}
```

JMH测试结果：
```JAVA
Benchmark   Mode   Score    Units
        testMethod  thrpt  64.579   ops/s
```


## 测试JSON工具的版本

同一款工具不同的版本性能差距往往比较明显，针对被测试的5种JSON解析工具选择了目前主流的版本，本文测试的结果也仅限于以下版本：

| Tool    |版本 | 
|---------| --- |
| FastJson2 | 2.0.52 | 
| FastJson| 1.2.83 |
| Jackson | 2.17.2 | 
| Gson    | 2.11.0 | 
| Hutool  | 5.8.23 | 

以下为各工具版本对应的Maven依赖：

```java
<dependency>
<groupId>com.alibaba.fastjson2</groupId>
<artifactId>fastjson2</artifactId>
<version>2.0.52</version>
</dependency>
<dependency>
<groupId>com.alibaba</groupId>
<artifactId>fastjson</artifactId>
<version>1.2.83</version>
</dependency>
<dependency>
<groupId>com.fasterxml.jackson.core</groupId>
<artifactId>jackson-core</artifactId>
<version>2.17.2</version>
</dependency>
<dependency>
<groupId>com.fasterxml.jackson.core</groupId>
<artifactId>jackson-databind</artifactId>
<version>2.17.2</version>
</dependency>
<dependency>
<groupId>com.google.code.gson</groupId>
<artifactId>gson</artifactId>
<version>2.11.0</version>
</dependency>
<dependency>
<groupId>cn.hutool</groupId>
<artifactId>hutool-json</artifactId>
<version>5.8.23</version>
</dependency>
```

## 测试平台

测试代码跑出的得分是依赖于JDK版本和运行代码的机器，不同机器跑出的得分差异是很大的，以下是我的测试机器、JDK版本和IDE版本：

- **硬件** : MacBook Pro 16GB 13英寸 M2 2022   macOS Ventura 13.5.1 (22G90)
- **JDK** : Azul Zulu 17.0.8 - aarch64
- **IDE** : IntelliJ IDEA 2024.2 (Ultimate Edition


## 测试代码

测试中我针对`小JSON`、`中JSON`、`大JSON`做序列化和反序列化跑分。其中小、中、大JSON我的定义为：

- **小JSON**

任何一个系统都会有用户信息，我想获取用户详情接口返回的用户信息JSON应该是最能代表我们日常项目开发中对`小JSON`的定义，所以我选择一条用户信息做为`小JSON`来做序列化和反序列化测试。以下为用户对象的定义：
```JAVA
@Data
public class User {
    private Long id;
    private String name;
    private String trueName;
    private Integer age;
    private String sex;
    private Date createTime;
}
```

- **中JSON**

在实际项目中我们除了有大量获取详情接口外，其次应该就是获取列表接口了，一般分页返回数据条数为10条或者20条，这里我选取20条用户信息做为我对`中JSON`的测试数据，我想这应该是非常具有代表性的`中JSON`数据。中JSON的数据定义：

```JAVA
private List<User> users;
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
        }
```

- **大JSON**

每个项目中`大JSON`可能都不一样，我以博客系统为例，我觉得`大JSON`可能就是文章正文中的HTML富文本数据，所以测试数据中的`大JSON`我选择了公众号文章详情页中的HTML富文本数据。以下为`大JSON`对象定义：

```java
@Data
public class Article {
    private Long id;
    private String author;
    private Long tenantId;
    private String title;
    private String subTitle;
    private String htmlContent;
    private Date publishTime;
}

    @Setup
    public void setup() throws IOException {
        article = new Article();
        article.setId(10000L);
        article.setTenantId(10000L);
        article.setAuthor("公众号：赵侠客");
        article.setPublishTime(new Date());
        article.setTitle(RandomUtil.randomString("主标题", 100));
        article.setSubTitle(RandomUtil.randomString("副标题", 50));
        article.setHtmlContent(new String(Files.readAllBytes(Paths.get("article.html"))));
    }
```

![大JSON部分数据内容](https://zhaochao-public.oss-cn-hangzhou.aliyuncs.com/images/20240906/51eb1a4096784c04938b42dcd8f384a9.png)



完成对`小JSON`,`中JSON`,`大JSON`数据的定义后，就可以使用`JMH`做基准测试了，以下为`小JSON`序列化测试代码：

```java
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
```



# JSON序列化测试

## 小JSON序列化测试

### 小JSON序列化跑分：

```java
Benchmark    Score        Units
        FastJson2    13653527.046 ops/s
        FastJson     8502829.931  ops/s
        Gson         1217934.274  ops/s
        HutoolJson   437293.524   ops/s
        Jackson      5779830.068  ops/s
```
从结果的值来看`小JSON`序列化都是非常快的，我们的HTTP接口响应时间基本上都是在几十毫秒到几秒之间，对`小JSON`做一次序列化可以说对我们的接口性能没有任何影响，如果你的项目只有这些小JSON感觉可以闭眼选工具，项目中引用了哪个就用哪个或者哪个用的习惯就用哪个，把主要精力放在业务上没必要太纠结JSON工具的选型。不过今天我们是`极客`，要有追究极致的精神，我对5种结果做了得分的排名，Score为JMH的跑分，

> **百分制**：最大Score的得100分，其它为 100\*(Score/最大值)


### 小JSON序列化排名：


| Tool    |Score | 百分制|
|---------| --- |------|
| FastJson2 | 13653527 | 100  |
| FastJson | 8502829 | 62.3 |
| Jackson | 5779830 | 42.3 |
| Gson    | 1217934 | 8.9  |
| Hutool  | 437293 | 3.2  |

看到这个排名后我有两点想说的：

- **FastJson2**，无敌是多么，多么寂寞
- **Hutool**，@所有人，大家看看自己在项目中有没有使用Hutool-Json，有用到的来下我办公室



## 中JSON序列化测试

### 中JSON序列化跑分:

```java
Benchmark   Score       Units
        FastJson2   236910.655  ops/s
        FastJson    173386.528  ops/s
        Gson        50937.391   ops/s
        HutoolJson  10928.165   ops/s
        Jackson     212457.203  ops/s
```

对于`中JSON`序列化来说分值就大幅度下降了，最差的Hutool 一秒只能序列化1万多次，也就是说做一次JSON转换需要0.1毫秒，做10次也就是1毫秒，如里接口中有大量中JSON序列化调用会对我们接口响应时间有一定影响。


### 中JSON序列化排名：

| Tool    |Score | 百分制|
|---------| --- |------|
| FastJson2 |236910 | 100  |
| Jackson | 212457 | 89.7 |
| FastJson| 173386 | 73.2 |
| Gson    | 50937 | 21.5 |
| Hutool  | 10928 | 4.6  |

看到这个排名后我有二点想说的：

- **FastJson2**，无敌是多么，多么空虚
- **Hutool**，@所有人，大家看看自己在项目中有没有使用Hutool-Json，有用到的来下我办公室



## 大JSON序列化测试

### 大JSON序列化跑分：

```java
Benchmark    Score     Units
        FastJson2   9650.211   ops/s
        FastJson    4791.032   ops/s
        Gson        5835.649   ops/s
        HutoolJson  1035.357   ops/s
        Jackson     13398.324  ops/s
```

大JSON的序列化得分已经降到最差的Hutool-Json执行一次需要1毫秒了，我这还是在M2上跑的，然后我又在PC电脑上跑了一下：
```
Benchmark   Score      Units
Fast2Json   5788.067   ops/s
FastJson    2480.132   ops/s
Gson        2176.535   ops/s
HutoolJson  455.914    ops/s
Jackson     5276.439   ops/s
```
上面是在 Intel(R) Core(TM) i7-4790K CPU @ 4.00GHz跑的结果，可以看出最差了执行一次JSON序列化需要2毫秒，所参`大JSON`解析的快慢非常影响我们的接口性能了。

### 大JSON序列化排名：

| Tool    |Score | 百分制|
|---------| --- |------|
| Jackson | 13398 | 100  |
| FastJson2 | 9650 | 72.0 |
| Gson    | 5835 | 43.6 |
| FastJson| 4791 | 35.8 |
| Hutool  | 1035 | 7.7  |

看到这个排名后我有二点想说的：

- **Jackson**，做为SpringBoot默认json序列化工具是有原因的
- **Hutool**，@所有人，大家看看自己在项目中有没有使用Hutool-Json，有用到的来下我办公室




# JSON反序列化测试


## 小JSON反序列化测试

### 小JSON反序列化跑分：

```java
Benchmark   Score        Units
        FastJson2  11654586.191  ops/s
        FastJson   5980216.867   ops/s
        Gson       2415733.238   ops/s
        HutoolJson 855421.710    ops/s
        Jackson    3194855.332   ops/s
```

### 小JSON反序化排名：


| Tool    |SS | SDS| 变化| 百分制|
|---------| --- |--- |--- |------|
| FastJson2   | 13653527 |  11654586  | -14.6%  |100  |
| FastJson    | 8502829 |  5980216  | -29.7%  |51.3 |
| Jackson | 1217934 |  3194855  | +162.3%  |27.4 |
| Gson    | 437293 |  2415733  | +452.4%  |20.7 |
| Hutool  |  5779830|  855421  | -85.2%  |7.3  |

其中：
- **SS( Small Serialize)** ，小JSON序列化跑分,
- **SDS(Small Deserializer)** ，小JSON反序列化跑分
- **变化**，相比自身小JSON序列化跑分增减百分比

看到这个排名后我有四点想说的：

- **FastJson2**，无敌是多么，多么寂寞
- **Jackson&Gson**，相比于序列化反序列化快多了
- **FastJson&FastJson2**，你很强但是却输给了自己
- **Hutool**，@所有人，大家看看自己在项目中有没有使用Hutool-Json，有用到的来下我办公室



## 中JSON反序列化测试

### 中JSON反序列化跑分：

```java
Benchmark  Score         Units
        FastJson2   691572.756   ops/s
        FastJson    495493.338   ops/s
        Gson        174852.543   ops/s
        HutoolJson  37997.839    ops/s
        Jackson     216731.673   ops/s
```

### 中JSON反序列化排名：


| Tool   |MS  |MDS | 变化|百分制|
|---------| --- | --- | --- |------|
| FastJson2  | 236910 | 691572  |+191.9%| 100  |
| FastJson   |173386  | 495493  |+185.8%| 71.6 |
| Jackson  |212457| 216731  |-2.0%| 31.3 |
| Gson     |50937| 174852  |+243.3%| 25.3 |
| Hutool   |50937| 37997 |-25.4% | 5.5  |

其中：
- **MS(Medium Serialize)** ，中JSON序列化跑分,
- **MDS(Medium  Deserializer)** ，中JSON反序列化跑分
- **变化**，相比自身中JSON序列化跑分增减百分比

看到这个排名后我有三点想说的：

- **FastJson2**，无敌是多么，多么空虚
- **FastJson2&FastJson**，不但强还比自己序列化强
- **Hutool**，@所有人，大家看看自己在项目中有没有使用Hutool-Json，有用到的来下我办公室



## 大JSON反序列化测试

### 大JSON反序列化跑分：

```java
Benchmark   Score     Units
        FastJson2   8555.106  ops/s
        FastJson    9002.889  ops/s
        Gson        6141.212  ops/s
        HutoolJson  1252.990  ops/s
        Jackson     4614.815  ops/s
```


### 大JSON反序列化排名：


| Tool    |BS |BDS | 变化| 百分制| 
|---------| ---| ---| --- |------|
| FastJson  |4791| 9002 |+87.9| 100  |
| FastJson2 |9650  | 8555 |-11.3| 95.0 |
| Gson   |5835 | 6141 |+5.2| 68.2 |
| Jackson |13398| 4614 |-65.6| 51.3 |
| Hutool  |1035| 1252 |+20.9| 13.9 |

其中：
-  **BS(Big Serialize)** ，大JSON序列化跑分
-  **BDS(Big Deserializer)** ，大JSON反序列化跑分
- **变化**，相比自身大JSON序列化跑分增减百分比

看到这个排名后我有三点想说的：

- **FastJson2**，青出于蓝而胜于蓝，可是你没想到人家还留了一手
- **FastJson**，教会徒弟饿死师傅这个道理你是懂的
- **Hutool**，@所有人，大家看看自己在项目中有没有使用Hutool-Json，有用到的来下我办公室



# 排行榜



| Tool      | 排名 | 总分    | 百分制  | SS      | MS   | BS    | SDS | MDS | BDS |
|-----------|---------|---------|------|------|----|----|----|-------|-----|
| FastJson2  |状元 | 567   | 100 | 100     | 100  | 72.0  | 100 | 100 | 95.0  |
| FastJson  | 榜眼 | 394.2 | 69.5 |62.3    | 73.2  | 35.8 | 51.3 | 71.6  | 100  | 
| Jackson    |探花 | 342   | 60.3 | 42.3    | 89.7  | 100  | 27.4 | 31.3  | 51.3 |
| Gson       |进士 | 188.2 | 33.2 |8.9     | 21.5  | 43.6  | 20.7  | 25.3 | 68.2  | 
| Hutool    | 孙山 | 42.2  | 7.4 |3.2     | 4.6   | 7.7   | 7.3  | 5.5  | 13.9  | 

其中：
- **排名**，根据总得分降序
- **总分**，6项得分总和
- **百分制**，总得分最大为100分， 100\*(最大总分-自己)/(最大总分)
- **SS**，小JSON序列化得分
- **MS**，中JSON序列化得分
- **BS**，大JSON序列化得分
- **SDS**，小JSON反序列化得分
- **MDS**，中JSON反序列化得分
- **BDS**，大JSON反序列化得分

![总排行榜](https://zhaochao-public.oss-cn-hangzhou.aliyuncs.com/images/20240907/993bc0b76d084437ac6681ea2223dc32.png)


看到这个排行榜后我有5点想说的：

- **FastJson2**，无敌是多么，多么寂寞、无敌是多么，多么空虚
- **FastJson**，长江后浪推前浪,前浪被拍在沙滩，你的漏洞那么多，该退休了
- **Jackson**，SpringBoot看上的没毛病
- **Gson**，你没存在感的原因要从别人找起，不是你不优秀，是优秀的人了太多了
- **Hutool**， @公众号 @赵侠客 你们两个工作交接一下，明天不用来了



最后本测试纯个人自娱自乐，由于本人开发水平有限，如果怀疑测试结果，可以评论区交流或者可以下载源码自己跑分：

> GitHub：[https://github.com/whzhaochao/JsonBenchmark](https://github.com/whzhaochao/JsonBenchmark)





