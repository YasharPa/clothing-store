package common;


public record PasswordPolicy(int minLength, boolean requireDigit, boolean requireUpper,
                             boolean requireSpecial) {
    public static PasswordPolicy defaults() {
        return new PasswordPolicy(8, true, true, false);
    }
}