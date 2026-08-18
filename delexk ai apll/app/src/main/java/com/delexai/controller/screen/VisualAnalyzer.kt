package com.delexai.controller.screen

import android.graphics.Bitmap
import android.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Analyzes frames from screen capture using OpenCV-like algorithms.
 * Detects visual patterns, color clusters, and object positions.
 * Pure Kotlin implementation for lightweight processing.
 *
 * PHASE 6 CONTINUATION - Visual Pattern Matching
 */
class VisualAnalyzer {

    private val scope = CoroutineScope(Dispatchers.Default)

    data class ColorMatch(
        val x: Int,
        val y: Int,
        val confidence: Float,
        val radius: Int
    )

    /**
     * Detects pixels matching a target RGB color within a tolerance threshold.
     *
     * @param bitmap The image to analyze
     * @param targetColor Target RGB color (format: 0xRRGGBB)
     * @param tolerance Color tolerance (0-255)
     * @return List of matching pixel coordinates
     */
    fun detectColorClusters(bitmap: Bitmap, targetColor: Int, tolerance: Int = 20): List<ColorMatch> {
        return try {
            val matches = mutableListOf<ColorMatch>()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

            val targetR = (targetColor shr 16) and 0xFF
            val targetG = (targetColor shr 8) and 0xFF
            val targetB = targetColor and 0xFF

            // First pass: find matching pixels
            val matchPixels = mutableSetOf<Int>()
            for (i in pixels.indices) {
                val pixel = pixels[i]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val colorDistance = sqrt(
                    ((r - targetR) * (r - targetR) +
                     (g - targetG) * (g - targetG) +
                     (b - targetB) * (b - targetB)).toDouble()
                ).toInt()

                if (colorDistance <= tolerance) {
                    matchPixels.add(i)
                }
            }

            // Second pass: cluster matching pixels
            val visited = mutableSetOf<Int>()
            for (pixelIndex in matchPixels) {
                if (!visited.contains(pixelIndex)) {
                    val cluster = floodFill(pixelIndex, matchPixels, visited, bitmap.width, bitmap.height)
                    if (cluster.size > 10) { // Minimum cluster size
                        val (centerX, centerY) = calculateClusterCenter(cluster, bitmap.width)
                        matches.add(
                            ColorMatch(
                                x = centerX,
                                y = centerY,
                                confidence = cluster.size.toFloat() / 100,
                                radius = calculateClusterRadius(cluster, centerX, centerY, bitmap.width)
                            )
                        )
                    }
                }
            }

            Timber.d("Detected ${matches.size} color clusters for color 0x${targetColor.toString(16)}")
            matches
        } catch (e: Exception) {
            Timber.e(e, "Error detecting color clusters")
            emptyList()
        }
    }

    /**
     * Flood fill algorithm to group adjacent matching pixels.
     *
     * @param startIndex Starting pixel index
     * @param matchPixels Set of all matching pixels
     * @param visited Set to track visited pixels
     * @param width Bitmap width for coordinate calculation
     * @param height Bitmap height for boundary checking
     * @return Set of pixel indices in the cluster
     */
    private fun floodFill(
        startIndex: Int,
        matchPixels: Set<Int>,
        visited: MutableSet<Int>,
        width: Int,
        height: Int
    ): Set<Int> {
        val cluster = mutableSetOf<Int>()
        val queue = ArrayDeque<Int>()

        queue.add(startIndex)
        visited.add(startIndex)

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            cluster.add(index)

            val x = index % width
            val y = index / width

            // Check 4 adjacent pixels
            val neighbors = listOf(
                if (x > 0) index - 1 else -1,
                if (x < width - 1) index + 1 else -1,
                if (y > 0) index - width else -1,
                if (y < height - 1) index + width else -1
            )

            for (neighbor in neighbors) {
                if (neighbor >= 0 && matchPixels.contains(neighbor) && !visited.contains(neighbor)) {
                    queue.add(neighbor)
                    visited.add(neighbor)
                }
            }
        }

        return cluster
    }

    /**
     * Calculates the center of mass for a pixel cluster.
     *
     * @param cluster Set of pixel indices
     * @param width Bitmap width
     * @return Pair of (centerX, centerY)
     */
    private fun calculateClusterCenter(cluster: Set<Int>, width: Int): Pair<Int, Int> {
        var sumX = 0
        var sumY = 0

        for (index in cluster) {
            val x = index % width
            val y = index / width
            sumX += x
            sumY += y
        }

        val centerX = sumX / cluster.size
        val centerY = sumY / cluster.size

        return Pair(centerX, centerY)
    }

    /**
     * Calculates the radius of a pixel cluster.
     *
     * @param cluster Set of pixel indices
     * @param centerX Center X coordinate
     * @param centerY Center Y coordinate
     * @param width Bitmap width
     * @return Average distance from center to cluster edge
     */
    private fun calculateClusterRadius(cluster: Set<Int>, centerX: Int, centerY: Int, width: Int): Int {
        var maxDistance = 0

        for (index in cluster) {
            val x = index % width
            val y = index / width

            val distance = sqrt(
                ((x - centerX) * (x - centerX) +
                 (y - centerY) * (y - centerY)).toDouble()
            ).toInt()

            if (distance > maxDistance) {
                maxDistance = distance
            }
        }

        return maxDistance
    }

    /**
     * Detects motion between two consecutive frames.
     *
     * @param previousFrame Previous frame bitmap
     * @param currentFrame Current frame bitmap
     * @param threshold Motion threshold (0-255)
     * @return List of regions with motion
     */
    fun detectMotion(previousFrame: Bitmap, currentFrame: Bitmap, threshold: Int = 20): List<Pair<Int, Int>> {
        return try {
            if (previousFrame.width != currentFrame.width || previousFrame.height != currentFrame.height) {
                Timber.w("Frame dimensions don't match for motion detection")
                return emptyList()
            }

            val motionRegions = mutableListOf<Pair<Int, Int>>()
            val prevPixels = IntArray(previousFrame.width * previousFrame.height)
            val currPixels = IntArray(currentFrame.width * currentFrame.height)

            previousFrame.getPixels(prevPixels, 0, previousFrame.width, 0, 0, previousFrame.width, previousFrame.height)
            currentFrame.getPixels(currPixels, 0, currentFrame.width, 0, 0, currentFrame.width, currentFrame.height)

            for (i in prevPixels.indices) {
                val prevColor = prevPixels[i]
                val currColor = currPixels[i]

                val prevR = (prevColor shr 16) and 0xFF
                val prevG = (prevColor shr 8) and 0xFF
                val prevB = prevColor and 0xFF

                val currR = (currColor shr 16) and 0xFF
                val currG = (currColor shr 8) and 0xFF
                val currB = currColor and 0xFF

                val diff = sqrt(
                    ((prevR - currR) * (prevR - currR) +
                     (prevG - currG) * (prevG - currG) +
                     (prevB - currB) * (prevB - currB)).toDouble()
                ).toInt()

                if (diff > threshold) {
                    val x = i % previousFrame.width
                    val y = i / previousFrame.width
                    motionRegions.add(Pair(x, y))
                }
            }

            Timber.d("Detected motion in ${motionRegions.size} pixels")
            motionRegions
        } catch (e: Exception) {
            Timber.e(e, "Error detecting motion")
            emptyList()
        }
    }

    /**
     * Checks if a point is within a circular region.
     *
     * @param x Point X coordinate
     * @param y Point Y coordinate
     * @param centerX Circle center X
     * @param centerY Circle center Y
     * @param radius Circle radius
     * @return true if point is within region
     */
    fun isPointInCircle(x: Int, y: Int, centerX: Int, centerY: Int, radius: Int): Boolean {
        val distance = sqrt(
            ((x - centerX) * (x - centerX) +
             (y - centerY) * (y - centerY)).toDouble()
        )
        return distance <= radius
    }

    /**
     * Cleans up resources.
     */
    fun destroy() {
        scope.cancel()
        Timber.d("VisualAnalyzer destroyed")
    }
}
