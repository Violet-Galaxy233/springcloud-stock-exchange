package com.itranswarp.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 小时级 K 线。
 */
@Entity
@Table(name = "hour_bars")
public class HourBarEntity extends AbstractBarEntity {
}
