package com.example.expensetracker.domain

import com.example.expensetracker.data.database.entities.AccountType
import com.example.expensetracker.domain.repository.ICategoryRepository
import com.example.expensetracker.domain.repository.ITransactionRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class WalletCalculatorTest {

    private lateinit var transactionRepository: ITransactionRepository
    private lateinit var categoryRepository: ICategoryRepository
    private lateinit var periodCalculator: PeriodCalculator
    private lateinit var walletCalculator: WalletCalculator

    @Before
    fun setup() {
        transactionRepository = Mockito.mock(ITransactionRepository::class.java)
        categoryRepository = Mockito.mock(ICategoryRepository::class.java)
        periodCalculator = PeriodCalculator(transactionRepository, categoryRepository)
        walletCalculator = WalletCalculator(transactionRepository, categoryRepository, periodCalculator)
    }

    @Test
    fun testValidateWalletMove_validMoves() {
        // Cash to Wallet
        assertTrue(walletCalculator.validateWalletMove(AccountType.CASH, AccountType.WALLET))
        // Bank to Wallet
        assertTrue(walletCalculator.validateWalletMove(AccountType.BANK, AccountType.WALLET))
        // Wallet to Cash
        assertTrue(walletCalculator.validateWalletMove(AccountType.WALLET, AccountType.CASH))
        // Wallet to Bank
        assertTrue(walletCalculator.validateWalletMove(AccountType.WALLET, AccountType.BANK))
    }

    @Test
    fun testValidateWalletMove_invalidMoves() {
        // Null destination
        assertFalse(walletCalculator.validateWalletMove(AccountType.CASH, null))
        assertFalse(walletCalculator.validateWalletMove(AccountType.WALLET, null))

        // Wallet to Wallet
        assertFalse(walletCalculator.validateWalletMove(AccountType.WALLET, AccountType.WALLET))

        // Cash to Bank (this is a regular transfer, not wallet move)
        assertFalse(walletCalculator.validateWalletMove(AccountType.CASH, AccountType.BANK))
        assertFalse(walletCalculator.validateWalletMove(AccountType.BANK, AccountType.CASH))
    }
}
