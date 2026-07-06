package com.itranswarp.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 日级 K 线。
 */
@Entity
@Table(name = "day_bars")
public class DayBarEntity extends AbstractBarEntity {
}
