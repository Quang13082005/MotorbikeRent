package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.controller.web.StaffPageController;
import com.motorrent.motorbikerental.dto.response.RentalOrderResponse;
import com.motorrent.motorbikerental.service.RentalOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/** Protect the operational queues while changing their presentation. No database required. */
class OperationsPageControllerTests {
    private RentalOrderService service;
    private StaffPageController controller;
    private ExtendedModelMap model;

    @BeforeEach
    void setup() {
        service = mock(RentalOrderService.class);
        controller = new StaffPageController(service);
        model = new ExtendedModelMap();
        when(service.getAllOrders()).thenReturn(List.of(
                order(1, "PENDING"), order(2, "CONFIRMED"), order(3, "READY_FOR_HANDOVER"),
                order(4, "HANDED_OVER"), order(5, "EXTENDED"), order(6, "OVERDUE"),
                order(7, "RETURN_REQUESTED"), order(8, "RETURNED"), order(9, "COMPLETED"),
                order(10, "CANCELLED"), order(11, "REJECTED"), order(12, "PENDING_PAYMENT")));
    }

    @Test
    void originalOrderQueueStillIncludesPaymentAndReturnStates() {
        assertThat(controller.rentalOrders(model)).isEqualTo("staff/rental-orders");
        assertThat(rentals()).extracting(RentalOrderResponse::getStatus)
                .contains("PENDING_PAYMENT", "RETURN_REQUESTED", "OVERDUE")
                .doesNotContain("COMPLETED", "CANCELLED", "REJECTED");
    }

    @Test
    void dashboardUsesRealOrderCountsAndSupportsAnEmptyQueue() {
        assertThat(controller.dashboard(model)).isEqualTo("staff/dashboard");
        assertThat(model.get("pendingCount")).isEqualTo(1);
        assertThat(model.get("preparingCount")).isEqualTo(2L);
        assertThat(model.get("rentingCount")).isEqualTo(2L);
        assertThat(model.get("overdueCount")).isEqualTo(1L);
        when(service.getAllOrders()).thenReturn(List.of());
        controller.dashboard(model);
        assertThat(model.get("pendingCount")).isEqualTo(0);
        assertThat((List<?>) model.get("pendingRentals")).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<RentalOrderResponse> rentals() {
        return (List<RentalOrderResponse>) model.get("rentals");
    }

    private static RentalOrderResponse order(long id, String status) {
        return RentalOrderResponse.builder().id(id).orderCode("TEST-" + id)
                .status(status).createdAt(LocalDateTime.of(2026, 9, 24, 8, 0)).build();
    }
}
