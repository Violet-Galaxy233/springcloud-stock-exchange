package com.itranswarp.exchange.model.quotation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 秒级 K 线。
 */
@Entity
@Table(name = "sec_bars")
public class SecBarEntity extends AbstractBarEntity {
}
