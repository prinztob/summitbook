package de.drtobiasprinz.summitbook.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.drtobiasprinz.summitbook.core.widget.WidgetUpdater
import de.drtobiasprinz.summitbook.widget.WidgetUpdaterImpl

@Module
@InstallIn(SingletonComponent::class)
abstract class WidgetModule {

    @Binds
    abstract fun bindWidgetUpdater(impl: WidgetUpdaterImpl): WidgetUpdater
}
