package com.itranswarp.exchange.tradingengine.assets;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.itranswarp.exchange.enums.AssetEnum;

/**
 * 资产系统测试：验证各类转账操作，并在任意操作后校验系统资产负债表恒为零。
 */
class AssetServiceTest {

    static final Long DEBT = 1L;
    static final Long USER_A = 100L;
    static final Long USER_B = 200L;

    AssetService service;

    @BeforeEach
    void setUp() {
        service = new AssetService();
        // 用户充值：由系统负债账户向用户转入，不检查负债账户余额:
        service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, DEBT, USER_A, AssetEnum.USD, bd("10000"), false);
        service.tryTransfer(Transfer.AVAILABLE_TO_AVAILABLE, DEBT, USER_B, AssetEnum.BTC, bd("10"), false);
    }

    @Test
    void deposit() {
        assertEquals(bd("10000"), service.getAsset(USER_A, AssetEnum.USD).getAvailable());
        assertEquals(bd("10"), service.getAsset(USER_B, AssetEnum.BTC).getAvailable());
        // 负债账户为负:
        assertEquals(bd("-10000"), service.getAsset(DEBT, AssetEnum.USD).getAvailable());
        assertEquals(bd("-10"), service.getAsset(DEBT, AssetEnum.BTC).getAvailable());
        assertBalanceZero();
    }

    @Test
    void freezeAndUnfreeze() {
        // 冻结 3000 USD 成功:
        assertTrue(service.tryFreeze(USER_A, AssetEnum.USD, bd("3000")));
        assertEquals(bd("7000"), service.getAsset(USER_A, AssetEnum.USD).getAvailable());
        assertEquals(bd("3000"), service.getAsset(USER_A, AssetEnum.USD).getFrozen());
        // 冻结超过可用余额失败:
        assertFalse(service.tryFreeze(USER_A, AssetEnum.USD, bd("8000")));
        // 状态不变:
        assertEquals(bd("7000"), service.getAsset(USER_A, AssetEnum.USD).getAvailable());
        // 解冻 1000:
        service.unfreeze(USER_A, AssetEnum.USD, bd("1000"));
        assertEquals(bd("8000"), service.getAsset(USER_A, AssetEnum.USD).getAvailable());
        assertEquals(bd("2000"), service.getAsset(USER_A, AssetEnum.USD).getFrozen());
        assertBalanceZero();
    }

    @Test
    void unfreezeTooMuchFails() {
        service.tryFreeze(USER_A, AssetEnum.USD, bd("1000"));
        // 解冻超过冻结额应抛异常:
        assertThrows(RuntimeException.class, () -> service.unfreeze(USER_A, AssetEnum.USD, bd("2000")));
        assertBalanceZero();
    }

    @Test
    void transferBetweenUsers() {
        service.transfer(Transfer.AVAILABLE_TO_AVAILABLE, USER_A, USER_B, AssetEnum.USD, bd("2500"));
        assertEquals(bd("7500"), service.getAsset(USER_A, AssetEnum.USD).getAvailable());
        assertEquals(bd("2500"), service.getAsset(USER_B, AssetEnum.USD).getAvailable());
        // 余额不足的转账应抛异常:
        assertThrows(RuntimeException.class,
                () -> service.transfer(Transfer.AVAILABLE_TO_AVAILABLE, USER_A, USER_B, AssetEnum.USD, bd("999999")));
        assertBalanceZero();
    }

    @Test
    void negativeAmountRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.tryFreeze(USER_A, AssetEnum.USD, bd("-1")));
    }

    /**
     * 校验：所有用户 (含负债账户) 每种资产的 (可用 + 冻结) 总和恒为 0。
     */
    void assertBalanceZero() {
        for (AssetEnum asset : AssetEnum.values()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Map.Entry<Long, ConcurrentMap<AssetEnum, Asset>> e : service.getUserAssets().entrySet()) {
                Asset a = e.getValue().get(asset);
                if (a != null) {
                    sum = sum.add(a.getAvailable()).add(a.getFrozen());
                }
            }
            assertEquals(0, sum.signum(), "Asset " + asset + " total must be zero but was " + sum);
        }
    }

    static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
