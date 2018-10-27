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

package ro.luca1152.gravitybox.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import ktx.math.minus
import ktx.math.times
import ktx.math.vec2
import ro.luca1152.gravitybox.components.*
import ro.luca1152.gravitybox.entities.EntityFactory
import ro.luca1152.gravitybox.utils.GameStage
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.lang.Math.pow

/**
 * A system that handles what happens when a bullet collides with a platform.
 */
class BulletCollisionSystem(private val playerEntity: Entity,
                            private val stage: GameStage = Injekt.get(),
                            private val world: World = Injekt.get()) : IteratingSystem(Family.all(BulletComponent::class.java, ImageComponent::class.java).get()) {
    override fun processEntity(bullet: Entity, deltaTime: Float) {
        if (bullet.bullet.collidedWithWall) {
            createExplosion(bullet)
            pushPlayer(bullet)
            removeBullet(bullet)
        }
    }

    private fun createExplosion(bullet: Entity) {
        engine.addEntity(EntityFactory.createExplosion(bullet.physics.body.worldCenter))
    }

    private fun pushPlayer(bullet: Entity) {
        val sourcePosition = vec2(x = bullet.physics.body.worldCenter.x, y = bullet.physics.body.worldCenter.y)
        val playerBody = playerEntity.physics.body
        val distance = playerBody.worldCenter.dst(sourcePosition).toDouble()
        var forceVector = playerBody.worldCenter.cpy()
        forceVector -= sourcePosition
        forceVector.nor()
        forceVector *= 1300 * (1.22f * pow(1 - .3, distance).toFloat()) * 5
        playerBody.applyForce(forceVector, playerBody.worldCenter, true)
    }

    private fun removeBullet(bullet: Entity) {
        stage.addAction(
                sequence(
                        delay(.01f), // With no delay there would be a gap between the bullet and the platform
                        removeActor(bullet.image)
                )
        )
        world.destroyBody(bullet.physics.body)
        engine.removeEntity(bullet)
    }
}