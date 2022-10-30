package com.example.Account.service;

import com.example.Account.domain.Account;
import com.example.Account.domain.AccountUser;
import com.example.Account.dto.AccountDto;
import com.example.Account.exception.AccountException;
import com.example.Account.repository.AccountRepository;
import com.example.Account.repository.AccountUserRepository;
import com.example.Account.type.AccountStatus;
import com.example.Account.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.example.Account.type.AccountStatus.*;
import static com.example.Account.type.AccountStatus.IN_USE;
import static com.example.Account.type.ErrorCode.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
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
    void createAccountFailed_UserNotFound() {
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
    void createAccountFailed_maxAccountIs10() {
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

    @Test
    @DisplayName("계좌 해지 성공")
    void deleteAccountSuccess() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L).name("ryureeru").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of((accountUser)));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1234567890")
                        .accountStatus(IN_USE)
                        .balance(0L).build()));
        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);


        // when
        AccountDto accountDto = accountService.deleteAccount(12L, "1111111111");

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(1L, accountDto.getUserId());
        assertEquals("1234567890", captor.getValue().getAccountNumber());
        assertEquals(UNREGISTERED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 사용자 없는 경우")
    void deleteAccountFailed_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1234567890"));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌 해지 실패 - 계좌가 없는 경우")
    void deleteAccountFailed_AccountNotFound() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L)
                .name("ryureeru").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(12L, "1234567890"));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌 해지 실패 - 사용자 아이디와 계좌 소유주가 다른 경우")
    void deleteAccountFailed_userUnMatch() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L)
                .name("ryureeru").build();
        AccountUser accountUser2 = AccountUser.builder()
                .id(2L)
                .name("heeju").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser2)
                        .accountNumber("1234567890")
                        .accountStatus(IN_USE)
                        .balance(0L).build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(12L, "1111111111"));

        // then
        assertEquals(USER_ACCOUNT_UNMATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 계좌가 이미 해지 상태인 경우")
    void deleteAccountFailed_alreadyUnregistered() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L)
                .name("ryureeru").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1234567890")
                        .accountStatus(UNREGISTERED)
                        .balance(0L).build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(12L, "1111111111"));

        // then
        assertEquals(ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());

    }

    @Test
    @DisplayName("계좌 해지 실패 - 잔액이 있는 경우 실패 응답")
    void deleteAccountFailed_balanceNotEmpty() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L)
                .name("ryureeru").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1234567890")
                        .accountStatus(IN_USE)
                        .balance(100L).build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(12L, "1111111111"));

        // then
        assertEquals(BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 조회 성공")
    void getAccountsByUserIdSuccess() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .id(1L).name("ryureeru").build();
        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1111111111")
                        .balance(100L)
                        .build(),
                Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1111111112")
                        .balance(200L)
                        .build(),
                Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1111111113")
                        .balance(300L)
                        .build()
        );
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        //when
        List<AccountDto> accountDtos = accountService.getAccountsByUserId(12L);

        //then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accountDtos.get(0).getAccountNumber());
        assertEquals(100, accountDtos.get(0).getBalance());
        assertEquals("1111111112", accountDtos.get(1).getAccountNumber());
        assertEquals(200, accountDtos.get(1).getBalance());
        assertEquals("1111111113", accountDtos.get(2).getAccountNumber());
        assertEquals(300, accountDtos.get(2).getBalance());

    }

    @Test
    @DisplayName("계좌 조회 실패 - 사용자 없는 경우")
    void accountsByUserIdFailed_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

    }
}