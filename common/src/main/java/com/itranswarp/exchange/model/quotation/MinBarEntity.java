package com.itranswarp.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 分钟级 K 线。
 */
@Entity
@Table(name = "min_bars")
public class MinBarEntity extends AbstractBarEntity {
}
