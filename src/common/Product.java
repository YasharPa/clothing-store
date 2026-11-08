package common;

import java.io.Serializable;

/**
 * A product the chain sells (the same in every branch; only the quantity differs per branch).
 * Immutable: all fields are final and there are no setters, so a Product can be shared
 * between threads and sent to clients without anyone changing it on the way.
 * Two products are equal when they have the same id.
 */
public final class Product implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final String name;
    private final Category category;
    private final double price;

    /**
     * @param id       the product number (catalog number), greater than 0
     * @param name     the name shown to employees, not empty
     * @param category the kind of product, not null
     * @param price    the regular price in shekels, greater than 0
     * @throws IllegalArgumentException if one of the values is not allowed
     */
    public Product(int id, String name, Category category, double price) {
        if (id <= 0) {
            throw new IllegalArgumentException("The product number must be greater than 0");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("The product name must not be empty");
        }
        if (category == null) {
            throw new IllegalArgumentException("The product category must not be null");
        }
        if (price <= 0) {
            throw new IllegalArgumentException("The price must be greater than 0");
        }
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Category getCategory() {
        return category;
    }

    public double getPrice() {
        return price;
    }

    /** Two products are the same product when their ids are the same. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Product product)) {
            return false;
        }
        return id == product.id;
    }

    /** Must agree with equals: same id, same hash code. */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return "#" + id + " " + name + " (" + category + ", " + price + ")";
    }
}
