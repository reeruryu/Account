package com.example.Account.service;

import com.example.Account.domain.Account;
import com.example.Account.domain.AccountUser;
import com.example.Account.domain.Transaction;
import com.example.Account.dto.TransactionDto;
import com.example.Account.exception.AccountException;
import com.example.Account.repository.AccountRepository;
import com.example.Account.repository.AccountUserRepository;
import com.example.Account.repository.TransactionRepository;
import com.example.Account.type.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.Account.type.AccountStatus.*;
import static com.example.Account.type.ErrorCode.*;
import static com.example.Account.type.TransactionResult.F;
import static com.example.Account.type.TransactionResult.S;
import static com.example.Account.type.TransactionType.CANCEL;
import static com.example.Account.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    private static final long CANCEL_AMOUNT = 200L;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void useBalanceSuccess() {
        AccountUser accountUser = AccountUser.builder()
                .id(1L).name("ryureeru").build();
        Account account = Account.builder()
                .accountUser(accountUser)
                .accountNumber("1000000000")
                .accountStatus(IN_USE)
                .balance(10000L)
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResult(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.useBalance(
                2L, "2000000000", USE_AMOUNT);
        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());

        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(1000L, transactionDto.getAmount());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());

    }

    @Test
    @DisplayName("잔액 사용 실패 - 사용자 없는 경우")
    void useBalanceFailed_UserNotFound() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        // then
        assertEquals(USER_NOT_FOUND, exception.getErrorCode());

    }

    @Test
    @DisplayName("잔액 사용 실패 - 사용자 아이디와 계좌 소유주가 다른 경우")
    void useBalanceFailed_userUnMatch() {
        // given
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        AccountUser park = AccountUser.builder()
                .id(13L).name("park").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(ryu));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(park)
                        .balance(10000L)
                        .accountNumber("1000000013").build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        // then
        assertEquals(USER_ACCOUNT_UNMATCH, exception.getErrorCode());

    }

    @Test
    @DisplayName("잔액 사용 실패 - 계좌가 이미 해지 상태인 경우")
    void useBalanceFailed_alreadyUnregistered() {
        // given
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(ryu));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(ryu)
                        .balance(10000L)
                        .accountStatus(AccountStatus.UNREGISTERED)
                        .accountNumber("1000000012").build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        // then
        assertEquals(ACCOUNT_ALREADY_UNREGISTERED, exception.getErrorCode());

    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래금액이 잔액보다 큰 경우")
    void useBalanceFailed_exceedAmount() {
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        Account account = Account.builder()
                .accountUser(ryu)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(100L)
                .build();
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(ryu));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, "1000000000", 1000L));

        // then
        verify(transactionRepository, times(0)).save(any());
        assertEquals(AMOUNT_EXCEED_BALANCE, exception.getErrorCode());

    }

    @Test
    @DisplayName("잔액 사용 취소 성공")
    void cancelBalanceSuccess() {
        // given
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        Account account = Account.builder()
                .accountUser(ryu)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResult(S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.cancelBalance(
                "transactionId", "1000000000", CANCEL_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());

        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 원 사용 거래 없음")
    void cancelBalanceFailed_TransactionNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transationId", "1000000000", 1000L));

        // then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancelBalanceFailed_AccountNotFound() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transationId", "1000000000", 1000L));

        // then
        assertEquals(ACCOUNT_NOT_FOUND, exception.getErrorCode());

    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 원거래 금액과 취소 금액이 다른 경우")
    void cancelBalanceFailed_CancelMustFully() {
        // given
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        Account account = Account.builder()
                .accountUser(ryu)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 100L)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", CANCEL_AMOUNT));

        // then
        assertEquals(CANCEL_MUST_FULLY, exception.getErrorCode());

    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 트랜잭션이 해당 계좌의 거래가 아닌 경우")
    void cancelBalanceFailed_TransactionAccountUnMatch() {
        // given
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        Account account = Account.builder()
                .accountUser(ryu)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Account accountNotUse = Account.builder()
                .accountUser(ryu)
                .accountNumber("1000000013")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L)
                .build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(accountNotUse));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionId", "1000000000", CANCEL_AMOUNT));

        // then
        assertEquals(TRANSACTION_ACCOUNT_UNMATCH, exception.getErrorCode());

    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void saveFailedUseBalanceSuccess() {
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        Account account = Account.builder()
                .accountUser(ryu)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L).build();
        given(accountRepository.findByAccountNumber(anyString()))
                .willReturn(Optional.of(account));
        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResult(F)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(10000L)
                        .balanceSnapshot(9000L)
                        .build());
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.saveFailedUseTransaction("1000000000", USE_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(F, captor.getValue().getTransactionResult());

    }

    @Test
    @DisplayName("거래 확인 성공")
    void queryTransactionSuccess() {
        // given
        AccountUser ryu = AccountUser.builder()
                .id(12L).name("ryu").build();
        Account account = Account.builder()
                .accountUser(ryu)
                .accountNumber("1000000012")
                .accountStatus(AccountStatus.IN_USE)
                .balance(10000L).build();
        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(CANCEL)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9800L)
                .build();
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        // when
        TransactionDto transactionDto = transactionService.queryTransaction(("trxId"));

        // then
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());

    }

    @Test
    @DisplayName("거래 확인 실패 - 해당 transaction_id 없는 경우")
    void queryTransactionFailed_TransactionNotFount() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        // then
        assertEquals(TRANSACTION_NOT_FOUND, exception.getErrorCode());

    }
}