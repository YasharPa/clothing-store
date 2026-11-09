package server;

import common.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Customer registration and updates (requirement 6). Customers belong to the whole chain, not to one branch.
 * In the full project this logic is part of StoreService; at the merge keep only one copy.
 */
public class CustomerService {
    private final Database db;
    private final LogService log = LogService.getInstance();

    public CustomerService(Database db) {
        this.db = db;
    }

    public List<Customer> list() {
        synchronized (db) {
            return new ArrayList<>(db.store().customers.values());
        }
    }

    public Customer get(String id) {
        synchronized (db) {
            Customer c = db.store().customers.get(id);
            if (c == null) {
                throw new ServiceException("A customer with this ID was not found");
            }
            return c;
        }
    }

    public Customer register(User actor, CustomerType type, String id, String fullName, String phone) {
        if (type == null) {
            throw new ServiceException("Please choose a customer type");
        }
        checkDetails(id, fullName, phone);
        Customer c;
        synchronized (db) {
            if (db.store().customers.containsKey(id)) {
                throw new ServiceException("A customer with this ID already exists");
            }
            c = Customer.create(type, id, fullName.trim(), phone.trim());
            db.store().customers.put(id, c);
            db.save();
        }
        log.log(LogType.CUSTOMER_REGISTRATION, actor.getUsername(), c.toString());
        return c;
    }

    public Customer update(User actor, String id, String fullName, String phone) {
        checkDetails(id, fullName, phone);
        Customer c;
        synchronized (db) {
            c = db.store().customers.get(id);
            if (c == null) {
                throw new ServiceException("A customer with this ID was not found");
            }
            c.setFullName(fullName.trim());
            c.setPhone(phone.trim());
            db.save();
        }
        log.log(LogType.CUSTOMER_UPDATED, actor.getUsername(), c.toString());
        return c;
    }

    /** What a purchase of a customer came to: the price after the promotion, and a text for the receipt. */
    public record Purchase(double paid, String description) {
    }

    /**
     * Applies the customer's promotion to a purchase, counts the purchase and moves the customer to the next
     * track if the customer earned it (requirement 6). The caller must hold the lock on the Database.
     */
    public Purchase recordPurchase(String customerId, double total) {
        Customer customer = get(customerId);
        double paid = customer.applyPromotion(total);
        String gift = customer.getGift(paid);
        customer.recordPurchase(paid);
        StringBuilder text = new StringBuilder();
        text.append(customer.getPromotionDescription());
        if (gift != null) {
            text.append(", gift: ").append(gift);
        }
        CustomerType next = customer.jobTransition();
        if (next != customer.getType()) {
            Customer upgraded = Customer.convert(customer, next);
            db.store().customers.put(upgraded.getId(), upgraded);
            text.append(". The customer is now: ").append(next.getLabel());
        }
        db.save();
        return new Purchase(paid, text.toString());
    }

    private void checkDetails(String id, String fullName, String phone) {
        if (id == null || !id.matches("\\d{9}")) {
            throw new ServiceException("The customer ID must be 9 digits");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new ServiceException("Please enter the customer's name");
        }
        if (phone == null || !phone.trim().matches("\\d{9,10}")) {
            throw new ServiceException("The phone number must be 9 or 10 digits");
        }
    }
}
