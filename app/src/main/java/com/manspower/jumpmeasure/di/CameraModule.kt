package com.manspower.jumpmeasure.di

import com.manspower.jumpmeasure.core.camera.CameraFrameSource
import com.manspower.jumpmeasure.core.camera.CameraXFrameSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CameraModule {

    @Binds
    abstract fun bindCameraFrameSource(
        cameraXFrameSource: CameraXFrameSource
    ): CameraFrameSource
}
