package com.legend.springbootredisdemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.hash.Jackson2HashMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/*
* https://docs.spring.io/spring-data/redis/docs/2.3.1.RELEASE/reference/html/
* 1. spring会自动加载配置文件里的配置项，然后创建所需要的实例并注入配置项
* 2. redis默认只允许本地连接，所以测试之前先关闭该保护模式
*    redis-cli -> config set protected-mode no
* 3. redis是二进制安全的，但java序列化字节数组的过程中java默认的序列化会加一些东西，所以使用StringRedisTemplate才能达到我们想要的效果
* 4. 可以用更低阶的api建立连接开始，但这是会涉及到字符的序列化与反序列的编码问题
* 5. 对于一个hash类型的数据我们更希望它取出来能自动变成我们想要的对象
* 6. 解决java.lang.Integer cannot be cast to java.lang.String -> 自定义序列化方式
* 7. 为了更便捷的调用redis，我们可以抽出，定义自己的template
* */

@Component
public class TestRedis {
    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    @Qualifier("MyTemplate")
    StringRedisTemplate myStringRedisTemplate;

    @Autowired
    ObjectMapper objectMapper;

    public void testValueOps(){
        redisTemplate.opsForValue().set("hello", "world");//此时在redis-cli中看到的是乱码，因为此时使用的是java默认的序列化方式，会加一些另外的东西
        System.out.println(redisTemplate.opsForValue().get("hello"));

        stringRedisTemplate.opsForValue().set("halo", "China");
        System.out.println(stringRedisTemplate.opsForValue().get("halo"));
    }

    public void testRedisConnection(){
        RedisConnection conn = stringRedisTemplate.getConnectionFactory().getConnection();
        conn.set("robin".getBytes(), "666".getBytes());
        System.out.println(new String(conn.get("robin".getBytes()))); //得到的是一个字符数组[B@5e8a459，需要自己转成字符串
    }

    public void testHashOps(){
        HashOperations<String, Object, Object> hash = stringRedisTemplate.opsForHash();
        hash.put("Luna", "name", "Nana");
        //hash.put("Luna", "age", 26); //java.lang.Integer cannot be cast to java.lang.String
        hash.put("Luna", "age", "26");
        System.out.println(hash.entries("Luna"));
        System.out.println(stringRedisTemplate.opsForValue().get("halo"));

        Person person = new Person();
        person.setName("Tom");
        person.setAge(36);
        /*Map<String, Object> personMap = new HashMap();
        personMap.put("name", person.getName());
        personMap.put("age", Integer.valueOf(person.getAge()).toString());//java.lang.Integer cannot be cast to java.lang.String
        stringRedisTemplate.opsForHash().putAll("tom", personMap);*/
        //上面述方法手动做转换太麻烦，有没有更简洁的方式？->引入第三库ObjectMapper
        Jackson2HashMapper jm = new Jackson2HashMapper(objectMapper, false);
        stringRedisTemplate.setHashValueSerializer(new Jackson2JsonRedisSerializer<Object>(Object.class));
        stringRedisTemplate.opsForHash().putAll("tom", jm.toHash(person));//在设置hash的序列化方式前会报错：java.lang.Integer cannot be cast to java.lang.String
        Map map = stringRedisTemplate.opsForHash().entries("tom");
        Person p = objectMapper.convertValue(map, Person.class);
        System.out.println(p.getName() + "-" + p.getAge());
    }

    public void testMyTemplate(){
        Person person = new Person();
        person.setName("Jack");
        person.setAge(56);
        Jackson2HashMapper jm = new Jackson2HashMapper(objectMapper, false);
        stringRedisTemplate.opsForHash().putAll("jack", jm.toHash(person));
        Map map = stringRedisTemplate.opsForHash().entries("jack");
        Person p = objectMapper.convertValue(map, Person.class);
        System.out.println(p.getName() + "-" + p.getAge());
    }

    public void testPS(){
        myStringRedisTemplate.convertAndSend("haha", "hello");//在此之前需通过redis-cli创建一个订阅者 subscribe haha

        RedisConnection conn = myStringRedisTemplate.getConnectionFactory().getConnection();//先开启订阅监听，然后再redis-cli发布消息：publish lala hello
        conn.subscribe(new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] bytes) {
                System.out.println(new String(message.getBody()));
            }
        }, "lala".getBytes());
        while (true){

        }
    }
}
