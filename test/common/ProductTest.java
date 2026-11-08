package common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    void keepsItsValues() {
        Product shirt = new Product(1, "Shirt", Category.SHIRTS, 59.9);

        assertEquals(1, shirt.getId());
        assertEquals("Shirt", shirt.getName());
        assertEquals(Category.SHIRTS, shirt.getCategory());
        assertEquals(59.9, shirt.getPrice());
    }

    @Test
    void refusesBadValues() {
        assertThrows(IllegalArgumentException.class, () -> new Product(0, "Shirt", Category.SHIRTS, 50));
        assertThrows(IllegalArgumentException.class, () -> new Product(1, "", Category.SHIRTS, 50));
        assertThrows(IllegalArgumentException.class, () -> new Product(1, "  ", Category.SHIRTS, 50));
        assertThrows(IllegalArgumentException.class, () -> new Product(1, "Shirt", null, 50));
        assertThrows(IllegalArgumentException.class, () -> new Product(1, "Shirt", Category.SHIRTS, 0));
        assertThrows(IllegalArgumentException.class, () -> new Product(1, "Shirt", Category.SHIRTS, -5));
    }

    @Test
    void productsWithTheSameIdAreEqual() {
        Product first = new Product(7, "Shirt", Category.SHIRTS, 50);
        Product sameId = new Product(7, "Another name", Category.PANTS, 80);

        assertEquals(first, sameId);
        assertEquals(first.hashCode(), sameId.hashCode());
    }

    @Test
    void productsWithDifferentIdsAreNotEqual() {
        assertNotEquals(new Product(1, "Shirt", Category.SHIRTS, 50), new Product(2, "Shirt", Category.SHIRTS, 50));
    }
}
