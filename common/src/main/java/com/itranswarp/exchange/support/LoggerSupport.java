package com.itranswarp.exchange.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提供 logger 的基类，业务类可继承以直接使用 {@code logger}。
 */
public abstract class LoggerSupport {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
}
