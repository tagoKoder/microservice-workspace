package com.tagokoder.account.application;

public class AccountNumberFmt {
    private AccountNumberFmt() {}
    public static String fmt12(Long n) {
    if (n == null) return "";
    return String.format("%012d", n);
    }
}
