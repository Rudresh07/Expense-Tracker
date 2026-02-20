package com.rudy.expensetracker.di

import androidx.room.Room
import com.rudy.expensetracker.analytics.FirebaseAnalytics
import com.rudy.expensetracker.database.AppDatabase
import com.rudy.expensetracker.repository.CategoryRepository
import com.rudy.expensetracker.repository.TransactionRepository
import com.rudy.expensetracker.utils.AuthManager
import com.rudy.expensetracker.utils.PreferenceManager
import com.rudy.expensetracker.viewmodel.CategoryViewModel
import com.rudy.expensetracker.viewmodel.TransactionViewmodel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appDiModule = module {

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "expense_tracker_db"
        ).build()
    }

    factory { get<AppDatabase>().expenseDao() }
    factory {get<AppDatabase>().categoryDao() }
    single { FirebaseAnalytics() }
    factory { TransactionRepository(get()) }
    factory { CategoryRepository(get()) }
    single{ PreferenceManager(androidContext()) }
    single { AuthManager(get(),get(),get()) }
    viewModel { TransactionViewmodel(get()) }
    viewModel { CategoryViewModel(get()) }
}
