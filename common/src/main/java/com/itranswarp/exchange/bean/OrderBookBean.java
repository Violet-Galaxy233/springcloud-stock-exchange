package com.itranswarp.exchange.bean;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单簿快照：某一时刻买卖双方各档挂单。
 */
public class OrderBookBean {

    public static final String EMPTY = "{\"price\":0,\"sequenceId\":0,\"buy\":[],\"sell\":[]}";

    /**
     * 对应的定序 ID。
     */
    public long sequenceId;

    /**
     * 最新成交价。
     */
    public BigDecimal price;

    /**
     * 买盘 (按价格从高到低)。
     */
    public List<OrderBookItemBean> buy;

    /**
     * 卖盘 (按价格从低到高)。
     */
    public List<OrderBookItemBean> sell;

    public OrderBookBean() {
    }

    public OrderBookBean(long sequenceId, BigDecimal price, List<OrderBookItemBean> buy, List<OrderBookItemBean> sell) {
        this.sequenceId = sequenceId;
        this.price = price;
        this.buy = buy;
        this.sell = sell;
    }
}
