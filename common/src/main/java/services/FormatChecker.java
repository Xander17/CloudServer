package services;

import resources.LoginRegError;

// TODO: 15.02.2020 Нужен от в модуле common? Или только в клиенте
public class FormatChecker {

    private LoginRegError currentError;

    public boolean checkLoginFormat(String s) {
        if (matchFail(s, ".{5,}", LoginRegError.LOGIN_MIN_LENGTH)) return false;
        else if (matchFail(s, "^[A-Za-z].*", LoginRegError.LOGIN_FIRST_LETTER)) return false;
        else if (matchFail(s, "\\w+", LoginRegError.LOGIN_LETTERS_DIGITS)) return false;
        else if (matchFail(s, ".{5,20}", LoginRegError.LOGIN_MAX_LENGTH)) return false;
        return true;
    }

    public boolean checkPasswordFormat(String s) {
        if (matchFail(s, ".{8,}", LoginRegError.PASS_MIN_LENGTH)) return false;
        else if (matchFail(s, ".*[A-ZА-Я].*", LoginRegError.PASS_CAPITAL_LETTER)) return false;
        else if (matchFail(s, ".*[a-zа-я].*", LoginRegError.PASS_LOWERCASE_LETTER)) return false;
        else if (matchFail(s, ".*\\d.*", LoginRegError.PASS_DIGITS)) return false;
        else if (matchFail(s, ".{8,30}", LoginRegError.PASS_MAX_LENGTH)) return false;
        return true;
    }

    private boolean matchFail(String strToMatch, String pattern, LoginRegError err) {
        if (!strToMatch.matches(pattern)) {
            currentError = err;
            return true;
        }
        return false;
    }

    public LoginRegError getCurrentError() {
        return currentError;
    }
}
