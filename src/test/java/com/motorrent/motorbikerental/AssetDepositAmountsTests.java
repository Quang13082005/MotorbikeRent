package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.entity.Payment;
import com.motorrent.motorbikerental.service.AssetDepositAmounts;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class AssetDepositAmountsTests {
    private Payment movement(String type, int amount, String direction, String status) {
        return Payment.builder().transactionType(type).amount(amount).receivedAmount(amount)
                .direction(direction).status(status).build();
    }
    @Test void higherDepositCollectsOnlyDifferenceAndRequiresActualStaffInput() {
        var quote = AssetDepositAmounts.quote(2000000, 1000000);
        assertThat(quote.additionalDeposit()).isEqualTo(1000000);
        assertThat(quote.heldAfterCollection()).isEqualTo(2000000);
        quote.validateActualCollection(1000000);
        assertThatThrownBy(() -> quote.validateActualCollection(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quote.validateActualCollection(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> quote.validateActualCollection(999999)).hasMessageContaining("Chưa thu đủ");
        assertThatThrownBy(() -> quote.validateActualCollection(2000000)).hasMessageContaining("vượt");
    }
    @Test void lowerOrEqualRequirementKeepsHeldDepositUntilFinalSettlement() {
        for (int required : List.of(0, 1500000, 2000000)) {
            var quote = AssetDepositAmounts.quote(required, 2000000);
            assertThat(quote.additionalDeposit()).isZero();
            assertThat(quote.heldAfterCollection()).isEqualTo(2000000);
            quote.validateActualCollection(0);
            assertThatThrownBy(() -> quote.validateActualCollection(1)).isInstanceOf(IllegalArgumentException.class);
        }
    }
    @Test void repeatedChangesUseNetPostedAssetLedgerNotRentalOrPendingAmounts() {
        var original = movement("ASSET_DEPOSIT", 1000000, "IN", "PAID");
        original.setComponentsJson("{\"ASSET_DEPOSIT\":1000000}");
        var rows = List.of(original,
                movement("ASSET_DEPOSIT_ADDITIONAL", 1000000, "IN", "PAID"),
                movement("ASSET_DEPOSIT_REFUND", 200000, "OUT", "PAID"),
                movement("RENTAL_REMAINING", 300000, "IN", "PAID"),
                movement("ASSET_DEPOSIT_ADDITIONAL", 900000, "IN", "PENDING"),
                movement("ASSET_DEPOSIT_ADDITIONAL", 900000, "IN", "REVIEW_REQUIRED"));
        assertThat(AssetDepositAmounts.held(rows)).isEqualTo(1800000);
        assertThat(AssetDepositAmounts.quote(2000000, AssetDepositAmounts.held(rows)).additionalDeposit()).isEqualTo(200000);
    }
    @Test void actualReceivedAmountIsAuthoritativeAndLegacyPurposeWorks() {
        var legacy = Payment.builder().purpose("SECURITY").amount(2000000).receivedAmount(1000000).status("PAID").direction("IN").build();
        assertThat(AssetDepositAmounts.held(List.of(legacy))).isEqualTo(1000000);
        legacy.setReceivedAmount(null);
        assertThat(AssetDepositAmounts.held(List.of(legacy))).isEqualTo(2000000);
    }
    @Test void inconsistentOrOverflowedBalanceIsNotSilentlyZeroed() {
        assertThatThrownBy(() -> AssetDepositAmounts.held(List.of(movement("ASSET_DEPOSIT_REFUND", 1, "OUT", "PAID"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssetDepositAmounts.held(List.of(movement("ASSET_DEPOSIT", Integer.MAX_VALUE, "IN", "PAID"), movement("ASSET_DEPOSIT_ADDITIONAL", 1, "IN", "PAID"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AssetDepositAmounts.quote(-1, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
