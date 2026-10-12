package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.entity.Payment;
import java.util.Collection;
import java.util.Set;

/** Asset money actually held, never the configured deposit or rental revenue. */
public final class AssetDepositAmounts {
    private AssetDepositAmounts() {}

    public static int held(Collection<Payment> payments) {
        long held = 0;
        for (var payment : payments) {
            if (!Financials.posted(payment)) continue;
            String type = Financials.type(payment);
            if (Set.of("ASSET_DEPOSIT", "ASSET_DEPOSIT_ADDITIONAL", "ASSET_DEPOSIT_REFUND").contains(type)) {
                held = Math.addExact(held, Financials.cash(payment));
            }
            // Deduction recognition belongs to Phase 7; do not invent a cash movement here.
        }
        if (held < 0 || held > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Số dư cọc tài sản cần được đối soát trước khi đổi xe");
        return (int) held;
    }

    public static Quote quote(int required, int held) {
        if (required < 0 || held < 0) throw new IllegalArgumentException("Tiền cọc tài sản không hợp lệ");
        int additional = Math.max(0, required - held);
        return new Quote(required, held, additional, Math.addExact(held, additional));
    }

    public record Quote(int requiredDeposit, int heldDeposit, int additionalDeposit, int heldAfterCollection) {
        public void validateActualCollection(Integer actual) {
            if (actual == null || actual < 0)
                throw new IllegalArgumentException("Nhập số tiền cọc thực thu, nhập 0 nếu không cần thu thêm");
            if (actual < additionalDeposit)
                throw new IllegalArgumentException("Chưa thu đủ phần cọc tài sản cần bổ sung");
            if (actual > additionalDeposit)
                throw new IllegalArgumentException("Tiền cọc thực thu vượt phần cần bổ sung; kiểm tra lại số cọc đang giữ");
        }
    }
}
