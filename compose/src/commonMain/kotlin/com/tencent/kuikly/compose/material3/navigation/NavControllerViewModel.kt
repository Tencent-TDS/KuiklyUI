/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.material3.navigation

import com.tencent.kuikly.lifecycle.ViewModelStore

/**
 * Centralized manager for [ViewModelStore] instances associated with [NavBackStackEntry]s.
 *
 * This follows the official Android Navigation component pattern where `NavControllerViewModel`
 * (an Activity-scoped ViewModel) holds a `Map<String, ViewModelStore>` keyed by each entry's
 * unique ID. This design ensures:
 *
 * 1. **Centralized lifecycle management**: All ViewModelStore instances are managed in one place,
 *    making cleanup reliable and preventing memory leaks.
 * 2. **Decoupled ownership**: [NavBackStackEntry] does not directly own its ViewModelStore;
 *    instead, it retrieves it via a provider function injected by [NavHostController].
 * 3. **Consistent cleanup**: When an entry is destroyed, its ViewModelStore is cleared and
 *    removed from this manager. When the NavController itself is destroyed, all stores are
 *    cleared.
 *
 * In the official Android implementation, this is an actual `ViewModel` scoped to the Activity,
 * which allows it to survive configuration changes. In KuiklyUI's KMP environment, there are
 * no configuration changes, so this is a simple class managed by [NavHostController].
 *
 * @see NavBackStackEntry
 * @see NavHostController
 */
internal class NavControllerViewModel {

    private val viewModelStores = mutableMapOf<String, ViewModelStore>()

    /**
     * Gets or creates a [ViewModelStore] for the given back stack entry [id].
     *
     * If a ViewModelStore already exists for this ID (e.g., the entry was restored),
     * the existing one is returned. Otherwise, a new one is created and stored.
     *
     * @param id The unique ID of the [NavBackStackEntry]
     * @return The [ViewModelStore] associated with this entry
     */
    fun getViewModelStore(id: String): ViewModelStore {
        return viewModelStores.getOrPut(id) { ViewModelStore() }
    }

    /**
     * Clears the [ViewModelStore] associated with the given back stack entry [id]
     * and removes it from this manager.
     *
     * This should be called when a [NavBackStackEntry] is fully destroyed (i.e., its
     * lifecycle has reached DESTROYED and any exit transitions have completed).
     *
     * @param id The unique ID of the [NavBackStackEntry] to clear
     */
    fun clear(id: String) {
        viewModelStores.remove(id)?.clear()
    }

    /**
     * Clears all [ViewModelStore] instances managed by this controller.
     *
     * This should be called when the [NavHostController] itself is being destroyed,
     * to ensure all ViewModels are properly cleaned up.
     */
    fun clearAll() {
        for (store in viewModelStores.values) {
            store.clear()
        }
        viewModelStores.clear()
    }

    override fun toString(): String {
        return "NavControllerViewModel(stores=${viewModelStores.keys})"
    }
}
