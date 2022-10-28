package com.example.Account.controller;

import com.example.Account.service.AccountService;
import com.example.Account.dto.AccountDto;
import com.example.Account.dto.CreateAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/account")
    public CreateAccount.Response CreateAccount(
            @RequestBody @Valid CreateAccount.Request request) {

        return CreateAccount.Response.from(accountService.createAccount(
                request.getUserId(),
                request.getInitialBalance()
        ));
    }
}
