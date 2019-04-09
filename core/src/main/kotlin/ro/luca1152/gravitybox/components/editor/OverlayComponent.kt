/*
 * This file is part of Gravity Box.
 *
 * Gravity Box is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Gravity Box is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Gravity Box.  If not, see <https://www.gnu.org/licenses/>.
 */

package ro.luca1152.gravitybox.components.editor

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool.Poolable
import ro.luca1152.gravitybox.components.ComponentResolver
import ro.luca1152.gravitybox.utils.kotlin.createComponent

/** Contains information regarding one entity's overlay. */
class OverlayComponent : Component, Poolable {
    /**
     * First level: move, rotate, delete
     * Second level: resize
     */
    var overlayLevel = 1
    var showMovementButtons = true
    var showRotationButton = true
    var showResizingButtons = true
    var showDeletionButton = true

    fun set(
        showMovementButtons: Boolean, showRotationButton: Boolean,
        showResizingButtons: Boolean, showDeletionButton: Boolean
    ) {
        this.showMovementButtons = showMovementButtons
        this.showRotationButton = showRotationButton
        this.showResizingButtons = showResizingButtons
        this.showDeletionButton = showDeletionButton
    }

    override fun reset() {
        showMovementButtons = true
        showRotationButton = true
        showResizingButtons = true
        showDeletionButton = true
        overlayLevel = 1
    }

    companion object : ComponentResolver<OverlayComponent>(OverlayComponent::class.java)
}

val Entity.overlay: OverlayComponent
    get() = OverlayComponent[this]

fun Entity.overlay(
    showMovementButtons: Boolean, showRotationButton: Boolean,
    showResizingButtons: Boolean, showDeletionButton: Boolean
) = add(createComponent<OverlayComponent>().apply {
    set(showMovementButtons, showRotationButton, showResizingButtons, showDeletionButton)
})!!