package com.fullsail.shoppingmadebetter.feature.household.di

import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepository
import com.fullsail.shoppingmadebetter.feature.household.data.HouseholdRepositoryImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.CreateHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.CreateHouseholdUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.GetHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.GetHouseholdUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.JoinHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.JoinHouseholdUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.LeaveHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.LeaveHouseholdUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.RegenerateInviteCodeUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.RegenerateInviteCodeUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.RemoveMemberUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.RemoveMemberUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.RenameHouseholdUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.RenameHouseholdUseCaseImpl
import com.fullsail.shoppingmadebetter.feature.household.domain.TransferHeadUseCase
import com.fullsail.shoppingmadebetter.feature.household.domain.TransferHeadUseCaseImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HouseholdModule {
    @Binds @Singleton
    abstract fun bindHouseholdRepository(impl: HouseholdRepositoryImpl): HouseholdRepository

    @Binds
    abstract fun bindGetHouseholdUseCase(impl: GetHouseholdUseCaseImpl): GetHouseholdUseCase

    @Binds
    abstract fun bindCreateHouseholdUseCase(impl: CreateHouseholdUseCaseImpl): CreateHouseholdUseCase

    @Binds
    abstract fun bindJoinHouseholdUseCase(impl: JoinHouseholdUseCaseImpl): JoinHouseholdUseCase

    @Binds
    abstract fun bindLeaveHouseholdUseCase(impl: LeaveHouseholdUseCaseImpl): LeaveHouseholdUseCase

    @Binds
    abstract fun bindRenameHouseholdUseCase(impl: RenameHouseholdUseCaseImpl): RenameHouseholdUseCase

    @Binds
    abstract fun bindTransferHeadUseCase(impl: TransferHeadUseCaseImpl): TransferHeadUseCase

    @Binds
    abstract fun bindRemoveMemberUseCase(impl: RemoveMemberUseCaseImpl): RemoveMemberUseCase

    @Binds
    abstract fun bindRegenerateInviteCodeUseCase(impl: RegenerateInviteCodeUseCaseImpl): RegenerateInviteCodeUseCase
}
