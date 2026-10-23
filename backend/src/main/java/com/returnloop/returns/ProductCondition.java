package com.returnloop.returns;

/**
 * Condition of a returned product as reported at intake, refined later by the
 * inspection result.
 */
public enum ProductCondition {

    /** Unopened, complete, indistinguishable from new. */
    NEW("New / sealed"),
    /** Opened but complete and functional, packaging may be damaged. */
   LIKE_NEW("Like new"),
    /** Opened and used, fully functional, minor wear. */
    GOOD("Good"),
    /** Functional but with visible wear or cosmetic damage. */
    FAIR("Fair"),
    /** Not functional, repair may be viable. */
    DAMAGED("Damaged"),
    /** Not functional, repair not viable. */
    UNSELLABLE("Unsellable"),
    /** Safety-critical failure; must not be resold. */
    UNSAFE("Unsafe");

    private final String label;

    ProductCondition(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
