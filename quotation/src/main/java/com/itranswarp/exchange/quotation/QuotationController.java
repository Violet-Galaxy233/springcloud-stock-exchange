package com.itranswarp.exchange.quotation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.itranswarp.exchange.model.quotation.AbstractBarEntity;
import com.itranswarp.exchange.quotation.db.DayBarRepository;
import com.itranswarp.exchange.quotation.db.HourBarRepository;
import com.itranswarp.exchange.quotation.db.MinBarRepository;
import com.itranswarp.exchange.quotation.db.SecBarRepository;
import com.itranswarp.exchange.redis.RedisCache;
import com.itranswarp.exchange.redis.RedisService;

/**
 * 行情查询接口：对外提供各粒度 K 线与最近成交查询。
 */
@RestController
@RequestMapping("/api/quotation")
public class QuotationController {

    @Autowired
    private SecBarRepository secBarRepository;

    @Autowired
    private MinBarRepository minBarRepository;

    @Autowired
    private HourBarRepository hourBarRepository;

    @Autowired
    private DayBarRepository dayBarRepository;

    @Autowired
    private RedisService redisService;

    /**
     * 最近的秒级 K 线。
     */
    @GetMapping("/bars/sec")
    public List<BigDecimal[]> getSecBars() {
        return toBarArrays(this.secBarRepository.findTop100ByOrderByStartTimeDesc());
    }

    /**
     * 最近的分钟级 K 线。
     */
    @GetMapping("/bars/min")
    public List<BigDecimal[]> getMinBars() {
        return toBarArrays(this.minBarRepository.findTop100ByOrderByStartTimeDesc());
    }

    /**
     * 最近的小时级 K 线。
     */
    @GetMapping("/bars/hour")
    public List<BigDecimal[]> getHourBars() {
        return toBarArrays(this.hourBarRepository.findTop100ByOrderByStartTimeDesc());
    }

    /**
     * 最近的日级 K 线。
     */
    @GetMapping("/bars/day")
    public List<BigDecimal[]> getDayBars() {
        return toBarArrays(this.dayBarRepository.findTop100ByOrderByStartTimeDesc());
    }

    /**
     * 最近成交 Tick 的 JSON (由行情服务写入 Redis)。
     */
    @GetMapping("/ticks")
    public String getRecentTicks() {
        String json = this.redisService.get(RedisCache.RecentTicks);
        return json == null ? "[]" : json;
    }

    /**
     * 将数据库倒序返回的 Bar 列表反转为按时间正序 (newest-last) 的数组列表。
     */
    private List<BigDecimal[]> toBarArrays(List<? extends AbstractBarEntity> bars) {
        List<BigDecimal[]> result = new ArrayList<>(bars.size());
        for (AbstractBarEntity bar : bars) {
            result.add(bar.toBarArray());
        }
        Collections.reverse(result);
        return result;
    }
}
