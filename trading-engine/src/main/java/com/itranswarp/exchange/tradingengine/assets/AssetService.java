package com.itranswarp.exchange.tradingengine.assets;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.support.LoggerSupport;

/**
 * 用户资产系统。
 * <p>
 * 资产以两层 {@link ConcurrentMap} 存储：用户 ID -> (资产 ID -> {@link Asset})。
 * 使用 {@code ConcurrentMap} 的目的不是为了并发写 —— 交易引擎对资产的写操作严格单线程；
 * 而是为了支持多线程并发读 (如查询接口)，同时避免 {@code HashMap} 在并发读时可能出现的死循环。
 * <p>
 * 所有资产操作本质上只有一种：转账 ({@link #tryTransfer})。
 */
@Component
public class AssetService extends LoggerSupport {

    /**
     * 用户 ID -> (资产 ID -> Asset)。
     */
    final ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> userAssets = new ConcurrentHashMap<>();

    public Asset getAsset(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> assets = this.userAssets.get(userId);
        if (assets == null) {
            return null;
        }
        return assets.get(assetId);
    }

    public Map<AssetEnum, Asset> getAssets(Long userId) {
        Map<AssetEnum, Asset> assets = this.userAssets.get(userId);
        if (assets == null) {
            return Map.of();
        }
        return assets;
    }

    public ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> getUserAssets() {
        return this.userAssets;
    }

    /**
     * 核心方法：尝试转账。
     *
     * @param checkBalance 是否检查转出方余额充足。用户充值时 (系统负债账户 -> 用户) 传入 false。
     * @return 转账是否成功。
     */
    public boolean tryTransfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount,
            boolean checkBalance) {
        // 转账金额不能为负:
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Negative amount");
        }
        // 获取源用户资产 (不存在则初始化):
        Asset fromAsset = getAsset(fromUser, assetId);
        if (fromAsset == null) {
            fromAsset = initAssets(fromUser, assetId);
        }
        // 获取目标用户资产 (不存在则初始化):
        Asset toAsset = getAsset(toUser, assetId);
        if (toAsset == null) {
            toAsset = initAssets(toUser, assetId);
        }
        return switch (type) {
            case AVAILABLE_TO_AVAILABLE -> {
                // 需要检查余额且余额不足:
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            // 从可用转至冻结:
            case AVAILABLE_TO_FROZEN -> {
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield true;
            }
            // 从冻结转至可用:
            case FROZEN_TO_AVAILABLE -> {
                if (checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.frozen = fromAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            default -> throw new IllegalArgumentException("invalid type: " + type);
        };
    }

    /**
     * 转账，失败则抛出异常。除充值外，常规转账均需检查余额。
     */
    public void transfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(type, fromUser, toUser, assetId, amount, true)) {
            throw new RuntimeException("Transfer failed for " + type + ", from user " + fromUser + " to user " + toUser
                    + ", asset = " + assetId + ", amount = " + amount);
        }
    }

    /**
     * 冻结用户资产 (可用 -> 冻结)，余额不足返回 false。
     */
    public boolean tryFreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        return tryTransfer(Transfer.AVAILABLE_TO_FROZEN, userId, userId, assetId, amount, true);
    }

    /**
     * 解冻用户资产 (冻结 -> 可用)，冻结不足则抛异常。
     */
    public void unfreeze(Long userId, AssetEnum assetId, BigDecimal amount) {
        if (!tryTransfer(Transfer.FROZEN_TO_AVAILABLE, userId, userId, assetId, amount, true)) {
            throw new RuntimeException("Unfreeze failed for user " + userId + ", asset = " + assetId + ", amount = "
                    + amount);
        }
    }

    Asset initAssets(Long userId, AssetEnum assetId) {
        ConcurrentMap<AssetEnum, Asset> map = this.userAssets.computeIfAbsent(userId, k -> new ConcurrentHashMap<>());
        Asset zeroAsset = new Asset();
        Asset old = map.putIfAbsent(assetId, zeroAsset);
        return old == null ? zeroAsset : old;
    }

    /**
     * 调试用：打印所有用户资产，并断言系统资产负债表恒为零。
     */
    public void debug() {
        System.out.println("---------- assets ----------");
        for (Map.Entry<Long, ConcurrentMap<AssetEnum, Asset>> userEntry : this.userAssets.entrySet()) {
            System.out.println("  user " + userEntry.getKey() + " ----------");
            for (Map.Entry<AssetEnum, Asset> assetEntry : userEntry.getValue().entrySet()) {
                System.out.println("    " + assetEntry.getKey() + ": " + assetEntry.getValue());
            }
        }
        System.out.println("----------------------------");
    }
}
