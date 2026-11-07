package common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Checks the three customer types: the discount, the gift, and when a customer moves up. */
class CustomerTest {

    @Test
    void newCustomerPaysNinetyPercent() {
        Customer customer = Customer.create(CustomerType.NEW, "100000001", "Dana", "0501111111");

        assertEquals(90.0, customer.applyPromotion(100.0), 0.001);
    }

    @Test
    void returningCustomerPaysNinetyFivePercent() {
        Customer customer = Customer.create(CustomerType.RETURNING, "100000002", "Gil", "0501111111", 3, 650);

        assertEquals(95.0, customer.applyPromotion(100.0), 0.001);
    }

    @Test
    void vipCustomerPaysSeventyFivePercent() {
        Customer customer = Customer.create(CustomerType.VIP, "100000003", "Liat", "0501111111", 12, 5400);

        assertEquals(75.0, customer.applyPromotion(100.0), 0.001);
    }

    @Test
    void onlyVipGetsAGift() {
        Customer newCustomer = Customer.create(CustomerType.NEW, "100000004", "Dana", "0501111111");
        Customer returning = Customer.create(CustomerType.RETURNING, "100000005", "Gil", "0501111111");
        Customer vip = Customer.create(CustomerType.VIP, "100000006", "Liat", "0501111111");

        assertNull(newCustomer.getGift(100));
        assertNull(returning.getGift(100));
        assertNotNull(vip.getGift(100));
    }

    @Test
    void newCustomerStaysNewUntilTheFirstPurchase() {
        Customer customer = Customer.create(CustomerType.NEW, "100000007", "Dana", "0501111111");

        assertEquals(CustomerType.NEW, customer.jobTransition());
    }

    @Test
    void newCustomerBecomesReturningAfterOnePurchase() {
        Customer customer = Customer.create(CustomerType.NEW, "100000008", "Dana", "0501111111");

        customer.recordPurchase(50);

        assertEquals(CustomerType.RETURNING, customer.jobTransition());
    }

    @Test
    void returningCustomerStaysReturningBeforeSixPurchases() {
        Customer customer = Customer.create(CustomerType.RETURNING, "100000009", "Gil", "0501111111", 4, 500);

        customer.recordPurchase(10);

        assertEquals(CustomerType.RETURNING, customer.jobTransition());
    }

    @Test
    void returningCustomerBecomesVipAtSixPurchases() {
        Customer customer = Customer.create(CustomerType.RETURNING, "100000010", "Gil", "0501111111", 5, 500);

        customer.recordPurchase(10);

        assertEquals(CustomerType.VIP, customer.jobTransition());
    }

    @Test
    void returningCustomerBecomesVipAtThirtyFiveHundredSpent() {
        Customer customer = Customer.create(CustomerType.RETURNING, "100000011", "Gil", "0501111111", 2, 3400);

        customer.recordPurchase(100);

        assertEquals(CustomerType.VIP, customer.jobTransition());
    }

    @Test
    void recordPurchaseCountsThePurchaseAndAddsTheAmount() {
        Customer customer = Customer.create(CustomerType.NEW, "100000012", "Dana", "0501111111");

        customer.recordPurchase(10.10);
        customer.recordPurchase(20.20);

        assertEquals(2, customer.getPurchaseCount());
        assertEquals(30.30, customer.getTotalSpent(), 0.001);
    }

    @Test
    void convertKeepsTheDetailsAndTheHistory() {
        Customer old = Customer.create(CustomerType.RETURNING, "100000013", "Gil Barak", "0522222222", 6, 700);

        Customer vip = Customer.convert(old, CustomerType.VIP);

        assertEquals(CustomerType.VIP, vip.getType());
        assertEquals("100000013", vip.getId());
        assertEquals("Gil Barak", vip.getFullName());
        assertEquals(6, vip.getPurchaseCount());
        assertEquals(700, vip.getTotalSpent(), 0.001);
    }
}
