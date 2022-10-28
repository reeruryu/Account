package com.example.Account.service;

import com.example.Account.domain.Account;
import com.example.Account.domain.AccountUser;
import com.example.Account.dto.AccountDto;
import com.example.Account.exception.AccountException;
import com.example.Account.repository.AccountRepository;
import com.example.Account.repository.AccountUserRepository;
import com.example.Account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.Account.type.AccountStatus.IN_USE;
import static com.example.Account.type.ErrorCode.MAX_ACCOUNT_PER_USER_10;
import static com.example.Account.type.ErrorCode.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("계좌 생성 성공")
    void createAccountSuccess() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L).name("ryureeru").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of((accountUser)));
        given(accountRepository.findFirstByOrderByIdDes())
                .willReturn(Optional.of(Account.builder()
                        .accountNumber("1111111111").build()));
        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1111111112").build());
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);


        // when
        AccountDto accountDto = accountService.createAccount(12L, 100L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1111111112", captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 사용자 없는 경우")
    void createAccount_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 100L));

        // then
        assertEquals(USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 계좌가 이미 10개")
    void createAccount_maxAccountIs10() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L).name("ryureeru").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of((accountUser)));
        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 100L));

        // then
        assertEquals(MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

}