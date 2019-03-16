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

package ro.luca1152.gravitybox.systems.editor

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import ro.luca1152.gravitybox.components.editor.SelectedObjectComponent
import ro.luca1152.gravitybox.components.game.ColorComponent
import ro.luca1152.gravitybox.components.game.ColorType
import ro.luca1152.gravitybox.components.game.ImageComponent
import ro.luca1152.gravitybox.components.game.color
import ro.luca1152.gravitybox.utils.kotlin.tryGet

/** Colors the selected map object accordingly. */
class SelectedObjectColorSystem :
    IteratingSystem(Family.all(ColorComponent::class.java, ImageComponent::class.java).get()) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        updateColor(entity)
    }

    private fun updateColor(entity: Entity) {
        if (entity.tryGet(SelectedObjectComponent) != null)
            entity.color.colorType = ColorType.DARKER_DARK
        else
            entity.color.colorType = ColorType.DARK
    }
}