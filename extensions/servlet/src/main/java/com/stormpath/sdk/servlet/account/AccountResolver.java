package com.stormpath.sdk.servlet.account;

import com.stormpath.sdk.account.Account;

import javax.servlet.ServletRequest;

public interface AccountResolver {

    boolean hasAccount(ServletRequest request);

    Account getAccount(ServletRequest request);
}
