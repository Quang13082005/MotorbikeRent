package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.entity.RentalOrder;
import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.repository.MotorbikeRepository;
import com.motorrent.motorbikerental.repository.RentalOrderRepository;
import com.motorrent.motorbikerental.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real template rendering and existing API transitions, isolated in a disposable H2 database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.datasource.url=jdbc:h2:mem:operations_test;MODE=MySQL;NON_KEYWORDS=YEAR;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "app.seed-demo=true", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.ai.openai.api-key=ui-test-placeholder", "spring.ai.openai.chat.enabled=false",
        "spring.ai.openai.embedding.enabled=false", "logging.file.name=target/ui-check/workflow-test.log"
})
@ActiveProfiles("preview")
@Transactional
class OperationsWorkflowTests {
    @Autowired private WebApplicationContext context;
    @Autowired private RentalOrderRepository orders;
    @Autowired private UserRepository users;
    @Autowired private MotorbikeRepository bikes;
    private MockMvc mvc;
    private RentalOrder order;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        var operator=users.findByUsername("staff_demo").orElseThrow();
        var principal=new com.motorrent.motorbikerental.security.CustomUserDetails(operator.getId(),operator.getUsername(),"STAFF","");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
        order = orders.saveAndFlush(RentalOrder.builder().orderCode("UI-REGRESSION-001")
                .customer(users.findByUsername("customer_demo").orElseThrow())
                .motorbike(bikes.findAll().get(0)).pickupDate(LocalDateTime.now().plusDays(1))
                .returnDate(LocalDateTime.now().plusDays(4)).days(3).totalPrice(450000).depositPaid(225000).balancePaid(225000)
                .status(RentalStatus.PENDING).build());
    }

    @org.junit.jupiter.api.AfterEach void clearAuth(){org.springframework.security.core.context.SecurityContextHolder.clearContext();}

    @Test
    void allOperationsTemplatesRenderAndIncludeTheirScopedAssets() throws Exception {
        for (String route : List.of("staff/dashboard", "staff/rental-orders", "staff/maintenance", "staff/profile", "admin/dashboard",
                "admin/rental-orders", "admin/motorbikes", "admin/maintenance", "admin/customers",
                "admin/staffs", "admin/reports", "admin/settings")) {
            String html = mvc.perform(get("/" + route)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).as(route).contains("/css/operations.css", "/js/operations.js", "id=\"mainContent\"");
        }
        assertThat(staffOrders()).contains("UI-REGRESSION-001", "staff-order-top", "staff-order-body",
                "data-action=\"confirm\"", "data-vehicle-change");
    }

    @Test
    void existingConfirmPrepareHandoverCompleteWorkflowStillWorks() throws Exception {
        String url = "/api/rental-orders/" + order.getId();
        mvc.perform(get(url)).andExpect(status().isOk());
        mvc.perform(post(url + "/confirm")).andExpect(status().isOk());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        assertThat(staffOrders()).contains("data-action=\"hand-over\"").doesNotContain("data-action=\"prepare-handover\"");
        order.setPickupDate(LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh")).minusMinutes(1));orders.saveAndFlush(order);
        mvc.perform(post(url + "/hand-over")).andExpect(status().isOk());
        assertThat(staffOrders()).contains("data-action=\"complete\"");
        mvc.perform(post(url + "/complete")).andExpect(status().isOk());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(staffOrders()).doesNotContain("UI-REGRESSION-001");
    }

    @Test
    void rejectingAnOrderPreservesTheReasonAndRemovesItFromTheActiveQueue() throws Exception {
        mvc.perform(post("/api/operations/orders/" + order.getId() + "/reject-with-refund")
                .contentType("application/json").content("{\"amount\":450000,\"method\":\"BANK_TRANSFER\",\"reference\":\"reject-ui-test\",\"reason\":\"Xe cần kiểm tra kỹ thuật\"}")).andExpect(status().isOk());
        var rejected = orders.findById(order.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(RentalStatus.REJECTED);
        assertThat(rejected.getRejectionReason()).isEqualTo("Xe cần kiểm tra kỹ thuật");
        assertThat(staffOrders()).doesNotContain("UI-REGRESSION-001");
    }

    @Test
    void inspectionPersistsAndHandoverLocksOriginalCondition() throws Exception {
        var url="/api/operations/orders/"+order.getId()+"/inspection";
        order.setStatus(RentalStatus.CONFIRMED);orders.saveAndFlush(order);
        mvc.perform(put(url).contentType("application/json").content("{\"before\":\"Xe nguyên vẹn\",\"odoBefore\":100}"))
                .andExpect(status().isOk());
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(jsonPath("before").value("Xe nguyên vẹn"))
                .andExpect(jsonPath("odoBefore").value(100));
        order.setStatus(RentalStatus.HANDED_OVER);orders.saveAndFlush(order);
        mvc.perform(put(url).contentType("application/json").content("{\"after\":\"Đã kiểm tra\",\"odoAfter\":130}"))
                .andExpect(status().isOk());
        mvc.perform(get(url)).andExpect(jsonPath("before").value("Xe nguyên vẹn"))
                .andExpect(jsonPath("odoBefore").value(100)).andExpect(jsonPath("odoAfter").value(130));
    }

    @Test
    void inspectionRejectsReturnBeforeHandoverAndRewritingDeliveryAfterHandover() throws Exception {
        var url="/api/operations/orders/"+order.getId()+"/inspection";
        order.setStatus(RentalStatus.CONFIRMED);orders.saveAndFlush(order);
        mvc.perform(put(url).contentType("application/json").content("{\"after\":\"Trả xe sớm\",\"odoAfter\":130}"))
                .andExpect(status().isBadRequest());
        order.setStatus(RentalStatus.HANDED_OVER);orders.saveAndFlush(order);
        mvc.perform(put(url).contentType("application/json").content("{\"before\":\"Ghi đè tình trạng ban đầu\",\"odoBefore\":500}"))
                .andExpect(status().isBadRequest());
    }

    @Autowired private com.motorrent.motorbikerental.repository.PaymentRepository payments;
    @Autowired private com.motorrent.motorbikerental.repository.RentalContractRepository contracts;

    @Test void handoverAllowsEarlyPickupWhenAvailableAndConflictFree() throws Exception {
        order.setStatus(RentalStatus.READY_FOR_HANDOVER);
        order.setBalancePaid(225000);order.setSecurityDeposit(0);
        orders.saveAndFlush(order);
        // Order pickup is tomorrow (plusDays(1)), but bike is AVAILABLE and conflict-free right now
        mvc.perform(post("/api/rental-orders/"+order.getId()+"/hand-over"))
            .andExpect(status().isOk());
        var updated = orders.findById(order.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RentalStatus.HANDED_OVER);
        assertThat(updated.getMotorbike().getStatus()).isEqualTo(com.motorrent.motorbikerental.enums.MotorbikeStatus.RENTED);
        var contract = contracts.findByRentalOrderId(order.getId()).orElseThrow();
        assertThat(contract.getHandedOverAt()).isNotNull();
        assertThat(contract.getHandoverStaffId()).isEqualTo(users.findByUsername("staff_demo").orElseThrow().getId());
    }

    @Test void handoverRejectsEarlyPickupWhenConflictOrUnavailable() throws Exception {
        // 1. If bike is UNAVAILABLE
        order.setStatus(RentalStatus.READY_FOR_HANDOVER);
        order.getMotorbike().setStatus(com.motorrent.motorbikerental.enums.MotorbikeStatus.UNAVAILABLE);
        bikes.saveAndFlush(order.getMotorbike());
        orders.saveAndFlush(order);
        mvc.perform(post("/api/rental-orders/"+order.getId()+"/hand-over"))
            .andExpect(status().isBadRequest());

        // 2. If there is a conflicting order during the early pickup window
        order.getMotorbike().setStatus(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE);
        bikes.saveAndFlush(order.getMotorbike());
        orders.saveAndFlush(RentalOrder.builder().orderCode("CONFLICT-001")
                .customer(users.findByUsername("customer_demo").orElseThrow())
                .motorbike(order.getMotorbike())
                .pickupDate(LocalDateTime.now().minusHours(1))
                .returnDate(LocalDateTime.now().plusDays(2))
                .days(2).totalPrice(300000).status(RentalStatus.CONFIRMED).build());
        mvc.perform(post("/api/rental-orders/"+order.getId()+"/hand-over"))
            .andExpect(status().isBadRequest());
    }

    @Test void handoverRecordsPaymentsAndAuditAtomically() throws Exception {
        order.setStatus(RentalStatus.CONFIRMED);order.setPickupDate(LocalDateTime.now().minusDays(1));
        order.setBalancePaid(0);order.setSecurityDeposit(1000000);orders.saveAndFlush(order);
        String url="/api/rental-orders/"+order.getId();
        mvc.perform(post(url+"/hand-over")).andExpect(status().isOk());
        var contract=contracts.findByRentalOrderId(order.getId()).orElseThrow();
        var handedAt=contract.getHandedOverAt();var handedBy=contract.getHandoverStaffId();
        assertThat(handedAt).isNotNull();assertThat(handedBy).isNotBlank();
        assertThat(order.getStatus()).isEqualTo(RentalStatus.HANDED_OVER);
        assertThat(order.getMotorbike().getStatus()).isEqualTo(com.motorrent.motorbikerental.enums.MotorbikeStatus.RENTED);

        var orderAfter = orders.findById(order.getId()).orElseThrow();
        assertThat(orderAfter.getBalancePaid()).isEqualTo(225000);

        var pList = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
        assertThat(pList).hasSize(2);
        assertThat(pList).anySatisfy(p -> {
            assertThat(p.getTransactionType()).isEqualTo("RENTAL_REMAINING");
            assertThat(p.getAmount()).isEqualTo(225000);
            assertThat(p.getDirection()).isEqualTo("IN");
            assertThat(p.getProcessedByStaffId()).isEqualTo(handedBy);
        });
        assertThat(pList).anySatisfy(p -> {
            assertThat(p.getTransactionType()).isEqualTo("ASSET_DEPOSIT");
            assertThat(p.getAmount()).isEqualTo(1000000);
            assertThat(p.getDirection()).isEqualTo("IN");
            assertThat(p.getProcessedByStaffId()).isEqualTo(handedBy);
        });

        // double handover must be rejected (idempotency)
        mvc.perform(post(url+"/hand-over")).andExpect(status().isBadRequest());

        var admin=users.findByUsername("admin_demo").orElseThrow();
        var principal=new com.motorrent.motorbikerental.security.CustomUserDetails(admin.getId(),admin.getUsername(),"ADMIN","");
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
        mvc.perform(post(url+"/complete")).andExpect(status().isOk());
        assertThat(order.getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(order.getMotorbike().getStatus()).isEqualTo(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE);
        assertThat(contract.getCompletionStaffId()).isEqualTo(admin.getId());assertThat(contract.getCompletedAt()).isEqualTo(order.getActualReturnDate());
        assertThat(contract.getHandedOverAt()).isEqualTo(handedAt);assertThat(contract.getHandoverStaffId()).isEqualTo(handedBy);
        mvc.perform(post(url+"/complete")).andExpect(status().isBadRequest());
        assertThat(staffOrders()).doesNotContain("UI-REGRESSION-001");
    }

    @Test void handoverWithCustomRequestSupportsBankTransferAndEarlyFee() throws Exception {
        order.setStatus(RentalStatus.CONFIRMED);
        order.setPickupDate(LocalDateTime.now().plusDays(1));
        order.setBalancePaid(0);order.setSecurityDeposit(500000);orders.saveAndFlush(order);

        String payload = """
            {
                "remainingAmount": 225000,
                "securityDeposit": 500000,
                "paymentMethod": "BANK_TRANSFER",
                "earlyPickupFee": 50000,
                "note": "Khách nhận sớm và chuyển khoản"
            }
            """;
        mvc.perform(post("/api/rental-orders/" + order.getId() + "/hand-over")
                .contentType("application/json").content(payload))
            .andExpect(status().isOk());

        var pList = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
        assertThat(pList).hasSize(3);
        assertThat(pList).anySatisfy(p -> {
            assertThat(p.getTransactionType()).isEqualTo("EARLY_PICKUP_FEE");
            assertThat(p.getAmount()).isEqualTo(50000);
            assertThat(p.getPaymentMethod()).isEqualTo("BANK_TRANSFER");
        });
    }

    @Test void handoverRejectsNegativeOrInsufficientAmounts() throws Exception {
        order.setStatus(RentalStatus.CONFIRMED);
        order.setBalancePaid(0);order.setSecurityDeposit(500000);orders.saveAndFlush(order);

        // Insufficient remaining amount
        String insufficient = """
            {"remainingAmount": 100000, "securityDeposit": 500000, "paymentMethod": "CASH"}
            """;
        mvc.perform(post("/api/rental-orders/" + order.getId() + "/hand-over")
                .contentType("application/json").content(insufficient))
            .andExpect(status().isBadRequest());

        // Negative deposit
        String negativeDeposit = """
            {"remainingAmount": 225000, "securityDeposit": -500000, "paymentMethod": "CASH"}
            """;
        mvc.perform(post("/api/rental-orders/" + order.getId() + "/hand-over")
                .contentType("application/json").content(negativeDeposit))
            .andExpect(status().isBadRequest());
    }
    @Test void cannotCompleteBeforeHandover() throws Exception {
        order.setStatus(RentalStatus.CONFIRMED);orders.saveAndFlush(order);
        mvc.perform(post("/api/rental-orders/"+order.getId()+"/complete")).andExpect(status().isBadRequest());
        assertThat(order.getStatus()).isEqualTo(RentalStatus.CONFIRMED);
    }

    private String staffOrders() throws Exception {
        return mvc.perform(get("/staff/rental-orders")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
