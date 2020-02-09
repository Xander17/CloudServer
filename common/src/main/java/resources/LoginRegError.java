package resources;

public enum LoginRegError {
    NO_CONNECTION("No connection to server"),
    NOT_ENOUGH_DATA("Please fill all fields"),
    INCORRECT_LOGIN_PASS("No such login or incorrect password"),
    LOGGED_ALREADY("User is already online"),
    LOGIN_EXISTS("Login already exists"),
    REG_ERROR("Registration fail"),
    DB_ERROR("DB error"),
    RESPONSE_ERROR("Server response error"),
    LOGIN_FIRST_LETTER("First symbol in login should be a letter"),
    LOGIN_LETTERS_DIGITS("Only word chars are allowed for login"),
    LOGIN_MIN_LENGTH("Login should be 5 chars minimum"),
    LOGIN_MAX_LENGTH("Login should be 20 chars maximum"),
    PASS_CAPITAL_LETTER("Password needs capital letters"),
    PASS_LOWERCASE_LETTER("Password needs lowercase letters"),
    PASS_DIGITS("Password needs digits"),
    PASS_MIN_LENGTH("Password should be 8 chars minimum"),
    PASS_MAX_LENGTH("Password should be 30 chars maximum");

    private String message;

    LoginRegError(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }

}
