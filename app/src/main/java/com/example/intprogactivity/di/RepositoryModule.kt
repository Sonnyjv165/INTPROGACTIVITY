package com.example.intprogactivity.di

import com.example.intprogactivity.data.repository.AuthRepositoryImpl
import com.example.intprogactivity.data.repository.BookingRepositoryImpl
import com.example.intprogactivity.data.repository.FlightRepositoryImpl
import com.example.intprogactivity.data.repository.PriceAlertRepositoryImpl
import com.example.intprogactivity.data.repository.UserRepositoryImpl
import com.example.intprogactivity.domain.repository.AuthRepository
import com.example.intprogactivity.domain.repository.BookingRepository
import com.example.intprogactivity.domain.repository.FlightRepository
import com.example.intprogactivity.domain.repository.PriceAlertRepository
import com.example.intprogactivity.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindFlightRepository(impl: FlightRepositoryImpl): FlightRepository

    @Binds @Singleton
    abstract fun bindBookingRepository(impl: BookingRepositoryImpl): BookingRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindPriceAlertRepository(impl: PriceAlertRepositoryImpl): PriceAlertRepository
}
