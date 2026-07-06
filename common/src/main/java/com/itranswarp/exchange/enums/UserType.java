package com.itranswarp.exchange.enums;

/**
 * 系统用户类型。
 */
public enum UserType {

    /**
     * 系统负债账户 (ID = 1)。所有用户权益之和记入该账户，
     * 以保证整个系统资产负债表恒为零，便于对账。
     */
    DEBT(1),

    /**
     * 普通交易用户。
     */
    TRADER(2);

    public final int value;

    UserType(int value) {
        this.value = value;
    }
}
