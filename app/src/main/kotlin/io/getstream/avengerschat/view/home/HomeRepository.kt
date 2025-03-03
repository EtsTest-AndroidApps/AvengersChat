/*
 * Copyright 2021 Stream.IO, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.getstream.avengerschat.view.home

import androidx.annotation.WorkerThread
import io.getstream.avengerschat.extensions.extraData
import io.getstream.avengerschat.extensions.liveRoomInfo
import io.getstream.avengerschat.model.Avenger
import io.getstream.avengerschat.persistence.AvengersDao
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.call.await
import io.getstream.chat.android.client.models.User
import io.getstream.chat.android.client.utils.onSuccessSuspend
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import javax.inject.Inject

class HomeRepository @Inject constructor(
    private val chatClient: ChatClient,
    private val avengersDao: AvengersDao,
    private val dispatcher: CoroutineDispatcher,
) {

    init {
        Timber.d("injection HomeRepository")
    }

    /**
     * Connects a specific user using the given user token to the Stream client server.
     * This connection will keep maintained until be disconnected.
     */
    @WorkerThread
    fun connectUser(avenger: Avenger) = flow {
        // disconnect a user if already connected.
        disconnectUser(avenger)

        val user = User(
            id = avenger.id,
            extraData = avenger.extraData
        )
        val result = chatClient.connectUser(user, avenger.token).await()
        result.onSuccessSuspend {
            emit(result.data())
        }
    }.flowOn(dispatcher)

    /**
     * Check and disconnect the current user
     * if there's already connected user to the Stream client server.
     */
    fun disconnectUser(avenger: Avenger) {
        val currentUser = chatClient.getCurrentUser()
        if (currentUser != null && avenger.id == currentUser.id) {
            chatClient.disconnect()
        }
    }

    /**
     * gets live room information list from the local database.
     */
    @WorkerThread
    fun getLiveRoomInfo(avenger: Avenger) = flow {
        val avengers = avengersDao.getAvengers()
        val liveRoomInfo = avengers.filterNot { it.id == avenger.id }.map { it.liveRoomInfo }
        emit(liveRoomInfo)
    }.flowOn(dispatcher)
}
