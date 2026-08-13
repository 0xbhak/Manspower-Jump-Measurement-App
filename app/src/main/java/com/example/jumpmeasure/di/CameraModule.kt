package com.example.jumpmeasure.di

import com.example.jumpmeasure.core.camera.CameraFrameSource
import com.example.jumpmeasure.core.camera.CameraXFrameSource
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
