package com.returnloop.inventory;

import com.returnloop.common.error.ApiException;
import com.returnloop.common.error.ErrorCode;
import com.returnloop.returns.ProductCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Stock must never go negative and must never be over-allocated. These are the
 * two rules the inventory service exists to enforce.
 */
class InventoryServiceTest {

    private InventoryItemRepository itemRepository;
    private InventoryMovementRepository movementRepository;
    private InventoryService service;

    @BeforeEach
    void setUp() {
        itemRepository = mock(InventoryItemRepository.class);
        movementRepository = mock(InventoryMovementRepository.class);
        service = new InventoryService(itemRepository, movementRepository);
    }

    private InventoryItem item(int quantity, int reserved) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID());
        item.setOrganizationId(UUID.randomUUID());
        item.setSku("NWR-PH-0142");
        item.setProductName("Aurora X1 Smartphone");
        item.setQuantity(quantity);
        item.setReservedQuantity(reserved);
        item.setUnitValue(new BigDecimal("600.00"));
        item.setStatus(InventoryStatus.AVAILABLE);
        item.setCondition(ProductCondition.GOOD);
        return item;
    }

    @Test
    @DisplayName("reserving more than the available quantity is rejected")
    void overReservationIsRejected() {
        InventoryItem item = item(10, 0);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.reserve(item.getOrganizationId(), item.getId(), 11, null, "tester"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CAPACITY_EXCEEDED));
    }

    @Test
    @DisplayName("reserving the exact available quantity is allowed")
    void exactReservationIsAllowed() {
        InventoryItem item = item(10, 4);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItemResponse response = service.reserve(item.getOrganizationId(), item.getId(), 6, null, "tester");

        assertThat(response.reservedQuantity()).isEqualTo(10);
        assertThat(response.availableQuantity()).isZero();
        assertThat(response.status()).isEqualTo(InventoryStatus.RESERVED);
    }

    @Test
    @DisplayName("an adjustment that would take stock below zero is rejected")
    void negativeAdjustmentIsRejected() {
        InventoryItem item = item(5, 0);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.adjust(item.getOrganizationId(), item.getId(), -6, "shrinkage", "tester"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("below zero");
    }

    @Test
    @DisplayName("an adjustment that would take stock below the reserved quantity is rejected")
    void adjustmentBelowReservedIsRejected() {
        InventoryItem item = item(5, 4);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.adjust(item.getOrganizationId(), item.getId(), -2, "shrinkage", "tester"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reserved quantity");
    }

    @Test
    @DisplayName("a positive adjustment increases the quantity and the value")
    void positiveAdjustmentIncreasesValue() {
        InventoryItem item = item(5, 0);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InventoryItemResponse response = service.adjust(item.getOrganizationId(), item.getId(), 3, "found", "tester");

        assertThat(response.quantity()).isEqualTo(8);
        assertThat(response.estimatedValue()).isEqualByComparingTo(new BigDecimal("4800.00"));
    }

    @Test
    @DisplayName("releasing more than is reserved is rejected")
    void overReleaseIsRejected() {
        InventoryItem item = item(10, 2);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.release(item.getOrganizationId(), item.getId(), 3, null, "tester"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("reserved");
    }

    @Test
    @DisplayName("a record from another organisation is not readable")
    void crossTenantAccessIsRejected() {
        InventoryItem item = item(5, 0);
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.require(UUID.randomUUID(), item.getId()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.CROSS_TENANT_ACCESS));
    }
}
