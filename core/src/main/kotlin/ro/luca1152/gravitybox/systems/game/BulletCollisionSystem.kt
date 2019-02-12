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

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.World
import ro.luca1152.gravitybox.components.BulletComponent
import ro.luca1152.gravitybox.components.ImageComponent
import ro.luca1152.gravitybox.components.bullet
import ro.luca1152.gravitybox.components.physics
import ro.luca1152.gravitybox.components.utils.removeAndResetEntity
import ro.luca1152.gravitybox.entities.EntityFactory
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get


/** Handles what happens when a bullet collides with a map object. */
class BulletCollisionSystem(private val playerEntity: Entity,
                            private val world: World = Injekt.get()) : IteratingSystem(Family.all(BulletComponent::class.java, ImageComponent::class.java).get()) {
    override fun processEntity(bullet: Entity, deltaTime: Float) {
        if (bullet.bullet.collidedWithPlatform) {
            EntityFactory.createExplosionImage(bullet.physics.body.worldCenter)
            applyBlastImpulse(bullet.physics.body)
            engine.removeAndResetEntity(bullet)
        }
    }

    private fun applyBlastImpulse(bullet: Body) {
        val playerBody = playerEntity.physics.body
        val closestBody = getClosestBodyToExplosion(bullet.worldCenter, playerBody.worldCenter)
        if (noObstacleFoundBetween(closestBody, playerBody))
            playerBody.applyBlastImpulse(bullet.worldCenter, playerBody.worldCenter, 150f)
    }

    private fun getClosestBodyToExplosion(explosionCenter: Vector2, playerCenter: Vector2): Body? {
        var closestBody: Body? = null
        world.rayCast({ fixture, _, _, fraction ->
            closestBody = fixture.body
            fraction
        }, explosionCenter, playerCenter)
        return closestBody
    }

    private fun noObstacleFoundBetween(closestBody: Body?, playerBody: Body) = closestBody == playerBody || closestBody == null

    private fun Body.applyBlastImpulse(blastCenter: Vector2, applyPoint: Vector2, blastPower: Float) {
        // Apply only on dynamic bodies so the impulse has an effect
        if (this.type != BodyDef.BodyType.DynamicBody)
            return

        // Calculate the distance
        val blastDir = applyPoint.cpy().sub(blastCenter)
        val distance = blastDir.len()

        // Calculate the inverse distance
        if (distance == 0f) return
        val invDistance = 1f / distance

        // Calculate the impulse's magnitude
        val impulseMag = Math.min(blastPower * invDistance * invDistance, 21f)

        // Apply the force
        this.applyLinearImpulse(
                blastDir.nor().scl(impulseMag),
                playerEntity.physics.body.worldCenter,
                true
        )
    }
}

