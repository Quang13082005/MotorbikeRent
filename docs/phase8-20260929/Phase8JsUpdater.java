import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Phase8JsUpdater {
    public static void main(String[] args) throws Exception {
        Path jsPath = Paths.get("src/main/resources/static/js/operations-live.js");
        String content = Files.readString(jsPath, StandardCharsets.UTF_8);

        String oldMetrics = "const metrics=[['revenue','Doanh thu đơn hoàn tất',true],['collected','Dòng tiền thuê và hoàn tiền',true],['maintenanceCost','Chi phí bảo dưỡng',true],['completedOrders','Đơn hoàn tất'],['totalBikes','Tổng số xe'],['activeRentals','Đơn đang thuê'],['paymentsToReview','Giao dịch cần đối soát']];";

        String newMetrics = "const metrics=[" +
                "['rentalNetRevenue','Doanh thu thuần thuê xe',true]," +
                "['operatingProfit','Lợi nhuận hoạt động',true]," +
                "['payosIn','Thu qua PayOS',true]," +
                "['cashIn','Thu tiền mặt (Staff)',true]," +
                "['bankTransferIn','Thu chuyển khoản (Staff)',true]," +
                "['rentalRefunded','Tiền hoàn khách thuê',true]," +
                "['lateFeeCollected','Phí trễ hạn thu được',true]," +
                "['totalAssetDepositHeld','Quỹ cọc tài sản đang giữ',true]," +
                "['assetDepositRefunded','Cọc tài sản đã hoàn',true]," +
                "['assetDepositDeducted','Cọc tài sản đã khấu trừ',true]," +
                "['maintenanceCost','Chi phí bảo dưỡng xe',true]," +
                "['completedOrders','Số đơn hoàn tất']," +
                "['paymentsToReview','Giao dịch cần đối soát']" +
                "];";

        if (!content.contains(oldMetrics)) {
            System.err.println("oldMetrics not found in operations-live.js!");
            return;
        }

        String updated = content.replace(oldMetrics, newMetrics);
        Files.writeString(jsPath, updated, StandardCharsets.UTF_8);
        System.out.println("operations-live.js updated successfully with Phase 8 metrics. Length: " + Files.size(jsPath));
    }
}
