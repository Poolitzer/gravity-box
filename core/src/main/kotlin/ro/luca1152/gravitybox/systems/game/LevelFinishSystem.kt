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

package ro.luca1152.gravitybox.systems.game

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import ro.luca1152.gravitybox.components.game.LevelComponent
import ro.luca1152.gravitybox.components.game.PlayerComponent
import ro.luca1152.gravitybox.components.game.level
import ro.luca1152.gravitybox.components.game.player
import ro.luca1152.gravitybox.utils.kotlin.approxEqualTo
import ro.luca1152.gravitybox.utils.kotlin.getSingletonFor
import ro.luca1152.gravitybox.utils.ui.Colors

/** Handles what happens when a level is finished. */
class LevelFinishSystem(private val restartLevelWhenFinished: Boolean = false) : EntitySystem() {
    private lateinit var levelEntity: Entity
    private lateinit var playerEntity: Entity

    // The color scheme is the one that tells whether the level was finished: if the current color scheme
    // is the same as the dark color scheme, then it means that the level was finished. I should change
    // this in the future.
    private val colorSchemeIsFullyTransitioned
        get() = (Colors.useDarkTheme && Colors.gameColor.approxEqualTo(Colors.LightTheme.game57))
                || (!Colors.useDarkTheme && Colors.gameColor.approxEqualTo(Colors.DarkTheme.game95))
    private val levelIsFinished
        get() = playerEntity.player.isInsideFinishPoint && colorSchemeIsFullyTransitioned

    override fun addedToEngine(engine: Engine) {
        levelEntity = engine.getSingletonFor(Family.all(LevelComponent::class.java).get())
        playerEntity = engine.getSingletonFor(Family.all(PlayerComponent::class.java).get())
    }

    override fun update(deltaTime: Float) {
        if (!levelIsFinished)
            return
        handleLevelFinish()
    }

    private fun handleLevelFinish() {
        if (restartLevelWhenFinished)
            levelEntity.level.restartLevel = true
        else {
            levelEntity.level.run {
                levelId++
                loadMap = true
                forceUpdateMap = true
            }
        }
    }

    override fun removedFromEngine(engine: Engine?) {
    }
}