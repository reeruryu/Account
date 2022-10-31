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
import com.example.Account.type.ErrorCode;
import com.example.Account.type.TransactionResult;
import com.example.Account.type.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.Account.type.AccountStatus.*;
import static com.example.Account.type.ErrorCode.*;
import static com.example.Account.type.TransactionResult.*;
import static com.example.Account.type.TransactionType.CANCEL;
import static com.example.Account.type.TransactionType.USE;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber, Long amount) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount);

        account.useBalance(amount);

        Transaction transaction = saveAndGetTransaction(USE, S, account, amount);

        return TransactionDto.fromEntity(transaction);
    }

    private void validateUseBalance(AccountUser accountUser, Account account,
                                    Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(ErrorCode.USER_ACCOUNT_UNMATCH);
        }

        if (account.getAccountStatus() == AccountStatus.UNREGISTERED) {
            throw new AccountException(ErrorCode.ACCOUNT_ALREADY_UNREGISTERED);
        }

        if (account.getBalance() < amount) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }

    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(USE, F, account, amount);
    }
    @Transactional
    public TransactionDto cancelBalance(String transactionId, String accountNumber, Long amount) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(CANCEL, S, account, amount));
    }

    private static void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(amount, transaction.getAmount())) {
            throw new AccountException(CANCEL_MUST_FULLY);
        }

        if (!Objects.equals(account.getAccountNumber(), transaction.getAccount().getAccountNumber())) {
            throw new AccountException(TRANSACTION_ACCOUNT_UNMATCH);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ErrorCode.ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(CANCEL, F, account, amount);
    }

    public TransactionDto queryTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(ErrorCode.TRANSACTION_NOT_FOUND));

        return TransactionDto.fromEntity(transaction);
    }

    private Transaction saveAndGetTransaction(
            TransactionType transactionType,
            TransactionResult transactionResult,
            Account account,
            Long amount) {
        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResult(transactionResult)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", "")) // uuid로 만들기
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }
}
