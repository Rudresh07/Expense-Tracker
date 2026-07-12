package com.rudy.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rudy.expensetracker.model.Transaction
import com.rudy.expensetracker.model.TransactionWithCategory
import com.rudy.expensetracker.repository.MerchantLearningRepository
import com.rudy.expensetracker.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TransactionViewmodel(
    private val repository: TransactionRepository,
    private val learningRepository: MerchantLearningRepository,
) : ViewModel() {

    // Expose a StateFlow for the list of all transactions

    init {
        loadTodayExpense()
    }
    private val _todayExpense = MutableStateFlow(0.0)
    val todayExpense: StateFlow<Double> = _todayExpense
    val transactionList: StateFlow<List<TransactionWithCategory>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L), // Keep subscribed for 5s after last collector stops
            initialValue = emptyList() // Initial value while data is loading
        )

    val totalBalance: StateFlow<Double> = repository.totalBalance
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0.0
        )

    val totalIncome: StateFlow<Double> = repository.totalIncome
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0.0
        )

    val totalExpense: StateFlow<Double> = repository.totalExpense
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0.0
        )

    val pendingReviewCount: StateFlow<Int> = repository.pendingReviewCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0,
        )

    val pendingReviewTransactions: StateFlow<List<TransactionWithCategory>> =
        repository.pendingReviewTransactions
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList(),
            )

    private val _showPendingReviewSheet = MutableStateFlow(false)
    val showPendingReviewSheet: StateFlow<Boolean> = _showPendingReviewSheet

    // Set once the sheet has been triggered this session; never reset so returning to
    // DashboardScreen doesn't re-show the sheet.
    private var pendingReviewShownThisSession = false

    fun onPendingReviewSheetShown() { _showPendingReviewSheet.value = false }

    fun triggerPendingReviewSheetIfNeeded(count: Int) {
        if (count > 0 && !pendingReviewShownThisSession) {
            pendingReviewShownThisSession = true
            _showPendingReviewSheet.value = true
        }
    }

    fun confirmCategory(txnId: Int, categoryId: Int, merchantKey: String, isWalletProxy: Boolean) {
        viewModelScope.launch {
            repository.updateCategoryAndClearReview(txnId, categoryId)
            if (!isWalletProxy && merchantKey.isNotBlank()) {
                learningRepository.saveConfirmation(merchantKey, categoryId)
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _filteredTransactionList = MutableStateFlow<List<TransactionWithCategory>>(emptyList())
    val filteredTransactionList: StateFlow<List<TransactionWithCategory>> = _filteredTransactionList

    private val _transactionDetail = MutableStateFlow<TransactionWithCategory?>(null)
    val transactionDetail: StateFlow<TransactionWithCategory?> = _transactionDetail

    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.addTransaction(transaction)
        }
    }

    fun getFilteredTransaction(month: String, year: String) {
        viewModelScope.launch {
            repository.getFilteredTransaction(month, year).collect { transactions ->
                _filteredTransactionList.value = transactions
            }
        }
    }
    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun getTransactionById(id: Int) {
        viewModelScope.launch {
            repository.getTransactionById(id).collect { transaction ->
                _transactionDetail.value = transaction
            }
        }
    }

    fun loadTodayExpense() {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MM yyyy"))

        viewModelScope.launch {
            repository.getTodayExpense(today).collect { expense ->
                _todayExpense.value = expense
            }
        }
    }
}