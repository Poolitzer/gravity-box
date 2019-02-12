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

import com.badlogic.ashley.core.EntitySystem
import ro.luca1152.gravitybox.metersToPixels
import ro.luca1152.gravitybox.utils.kotlin.GameCamera
import ro.luca1152.gravitybox.utils.kotlin.OverlayCamera
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Moves the [overlayCamera] along with the [gameCamera].*/
class OverlayCameraSyncSystem(private val overlayCamera: OverlayCamera = Injekt.get(),
                              private val gameCamera: GameCamera = Injekt.get()) : EntitySystem() {
    override fun update(deltaTime: Float) {
        overlayCamera.position.set(gameCamera.position.x.metersToPixels, gameCamera.position.y.metersToPixels, 0f)
        overlayCamera.update()
    }
}