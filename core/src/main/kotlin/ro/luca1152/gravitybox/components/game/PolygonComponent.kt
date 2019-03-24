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

package ro.luca1152.gravitybox.components.game

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Polygon
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Pool.Poolable
import ro.luca1152.gravitybox.components.ComponentResolver
import ro.luca1152.gravitybox.engine
import ro.luca1152.gravitybox.utils.kotlin.bottommostY
import ro.luca1152.gravitybox.utils.kotlin.leftmostX
import ro.luca1152.gravitybox.utils.kotlin.rightmostX
import ro.luca1152.gravitybox.utils.kotlin.topmostY

/** Contains a [Polygon]. */
class PolygonComponent : Component, Poolable {
    private var linkedImage = Image()
    var polygon = Polygon()
    var leftmostX = Float.POSITIVE_INFINITY
    var rightmostX = Float.NEGATIVE_INFINITY
    var bottommostY = Float.POSITIVE_INFINITY
    var topmostY = Float.NEGATIVE_INFINITY

    fun set(linkedImage: Image) {
        this.linkedImage = linkedImage
    }

    fun update() {
        updatePolygon()
        updateBounds()
    }

    private fun updatePolygon() {
        updateVertices()
        polygon.run {
            setPosition(linkedImage.x, linkedImage.y)
            setOrigin(linkedImage.originX, linkedImage.originY)
            rotation = linkedImage.rotation
        }
    }

    private fun updateVertices() {
        polygon.vertices = floatArrayOf(
            0f, 0f,
            linkedImage.width, 0f,
            linkedImage.width, linkedImage.height,
            0f, linkedImage.height
        )
    }

    private fun updateBounds() {
        resetBounds()
        leftmostX = polygon.leftmostX
        rightmostX = polygon.rightmostX
        bottommostY = polygon.bottommostY
        topmostY = polygon.topmostY
    }

    private fun resetBounds() {
        leftmostX = Float.POSITIVE_INFINITY
        rightmostX = Float.NEGATIVE_INFINITY
        bottommostY = Float.POSITIVE_INFINITY
        topmostY = Float.NEGATIVE_INFINITY
    }

    fun expandPolygonWith(left: Float = 0f, right: Float = 0f, top: Float = 0f, bottom: Float = 0f) {
        val transformedVertices = polygon.transformedVertices
        val arrayX = transformedVertices.filterIndexed { index, _ -> index % 2 == 0 }
        val arrayY = transformedVertices.filterIndexed { index, _ -> index % 2 == 1 }
        val sortedX = arrayX.sortedBy { it }
        val sortedY = arrayY.sortedBy { it }

        val leftmostIndex = arrayX.indexOf(sortedX.first()) * 2
        val secondLeftmostIndex = arrayX.lastIndexOf(sortedX[1]) * 2

        val rightmostIndex = arrayX.indexOf(sortedX.last()) * 2
        val secondRightmostIndex = arrayX.lastIndexOf(sortedX[2]) * 2

        val bottommostIndex = arrayY.indexOf(sortedY.first()) * 2
        val secondBottommostIndex = arrayY.lastIndexOf(sortedY[1]) * 2

        val topmostIndex = arrayY.indexOf(sortedY.last()) * 2
        val secondTopmostIndex = arrayY.lastIndexOf(sortedY[2]) * 2

        if (left != 0f) {
            polygon.vertices[leftmostIndex] += left * Math.signum(MathUtils.cosDeg(polygon.rotation))
            polygon.vertices[secondLeftmostIndex] += left * Math.signum(MathUtils.cosDeg(polygon.rotation))
        }
        if (right != 0f) {
            polygon.vertices[rightmostIndex] += right * Math.signum(MathUtils.cosDeg(polygon.rotation))
            polygon.vertices[secondRightmostIndex] += right * Math.signum(MathUtils.cosDeg(polygon.rotation))
        }
        if (top != 0f) {
            polygon.vertices[topmostIndex] += top * Math.signum(MathUtils.sinDeg(polygon.rotation))
            polygon.vertices[secondTopmostIndex] += top * Math.signum(MathUtils.sinDeg(polygon.rotation))
        }
        if (bottom != 0f) {
            polygon.vertices[bottommostIndex] += bottom * Math.signum(MathUtils.sinDeg(polygon.rotation))
            polygon.vertices[secondBottommostIndex] += bottom * Math.signum(MathUtils.sinDeg(polygon.rotation))
        }
        polygon.dirty()
    }

    override fun reset() {
        polygon = Polygon()
        linkedImage = Image()
        resetBounds()
    }

    companion object : ComponentResolver<PolygonComponent>(PolygonComponent::class.java)
}

val Entity.polygon: PolygonComponent
    get() = PolygonComponent[this]

fun Entity.polygon(linkedImage: Image) =
    add(engine.createComponent(PolygonComponent::class.java).apply {
        set(linkedImage)
    })!!

fun Entity.polygon() =
    add(engine.createComponent(PolygonComponent::class.java))!!