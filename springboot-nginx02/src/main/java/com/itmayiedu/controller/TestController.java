package com.itmayiedu.controller;

import com.google.common.hash.Funnels;
import com.itmayiedu.bloom.BloomFilterHelper;
import com.itmayiedu.bloom.RedisBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.util.*;

@RestController
public class TestController {
    @Autowired
    private RedisBloomFilter redisBloomFilter;

    @GetMapping("/test")
    public void index() {
        /**
         * 请求的数据
         */
        int expectedInsertions = 50000;
        /**
         * 误差率
         */
        double fpp = 0.1;
        redisBloomFilter.delete("bloom");
        BloomFilterHelper<CharSequence> bloomFilterHelper = new BloomFilterHelper<>(Funnels.stringFunnel(Charset.defaultCharset()), expectedInsertions, fpp);
        int rightNum = 0;
        int wrongNum = 0;
        // 添加10000个元素
        List<String> valueList = new ArrayList<>();
        List<String> keyList = new ArrayList<>();
        for (int i = 0; i < expectedInsertions / 5; i++) {
            String uuid = UUID.randomUUID().toString();
            valueList.add(uuid);
            keyList.add(uuid);
        }
        long beginTime = System.currentTimeMillis();
        redisBloomFilter.addList(bloomFilterHelper, "bloom", valueList);
        long costMs = (System.currentTimeMillis() - beginTime) / 1000;
        System.out.println("布隆过滤器添加" + expectedInsertions / 5 + "个值，耗时：" + costMs + "s");
        for (int i = 0; i < 10000; i++) {
            // 0-10000之间，可以被100整除的数有100个（100的倍数）
            String data = i % 100 == 0 ? keyList.get(i / 100) : UUID.randomUUID().toString();
            boolean result = redisBloomFilter.contains(bloomFilterHelper, "bloom", data);
            if (result) {
                if (keyList.contains(data)) {
                    rightNum++;
                    continue;
                }
                wrongNum++;
            }
        }
        BigDecimal percent = new BigDecimal(wrongNum).divide(new BigDecimal(9900), 2, RoundingMode.HALF_UP);
        BigDecimal bingo = new BigDecimal(9900 - wrongNum).divide(new BigDecimal(9900), 2, RoundingMode.HALF_UP);
        System.out.println("在5W个元素中，判断100个实际存在的元素，布隆过滤器认为存在的：" + (rightNum+wrongNum));
        System.out.println("在5W个元素中，判断9900个实际不存在的元素，误认为存在的：" + wrongNum + "，命中率：" + bingo + "，误判率：" + percent);
        System.out.println("验证结果耗时：" + (System.currentTimeMillis() - beginTime) / 1000 + "s");
    }
}
