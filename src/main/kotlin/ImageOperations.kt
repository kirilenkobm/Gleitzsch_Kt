import java.awt.image.BufferedImage

// Handles complex image transformations and manipulations.


class ImageOperations {
    fun extractChannel(imageArr: Array<Array<Array<Int>>>, channel: Int): Array<Array<Int>> {
        require(channel in 0..2) { "Channel must be between 0 and 2" }
        val width = imageArr.size
        val height = imageArr[0].size

        return Array(width) { i ->
            Array(height) { j ->
                imageArr[i][j][channel]
            }
        }
    }

    fun flattenChannel(channel: Array<Array<Int>>): Array<Int> {
        // need this exact flattering direction to get horizontal streaks, not vertical
        val flattened = mutableListOf<Int>()
        val numColumns = channel[0].size
        for (j in 0 until numColumns) {
            for (i in channel.indices) {
                flattened.add(channel[i][j])
            }
        }
        return flattened.toTypedArray()
    }

    fun shiftImage(matrix: Array<Array<Array<Int>>>, shift: Int): Array<Array<Array<Int>>> {
        // shift all pixels to the right by "shift"
        // needed to recover
        val height = matrix.size
        val width = matrix[0].size
        val depth = matrix[0][0].size
        val newMatrix = Array(height) { Array(width) { Array(depth) { 0 } } }

        for (h in 0 until height) {
            val newH = (h + shift) % height
            for (w in 0 until width) {
                for (d in 0 until depth) {
                    newMatrix[newH][w][d] = matrix[h][w][d]
                }
            }
        }
        return newMatrix
    }

    private fun percentile(arr: IntArray, percentile: Double): Int {
        val index = (percentile / 100) * arr.size
        return arr.sorted()[index.toInt()]
    }

    private fun rescaleIntensity(image: Array<Array<Array<Int>>>, inRange: Pair<Int, Int>): Array<Array<Array<Int>>>
    {
        val (low, high) = inRange
        val scaleFactor = 255.0 / (high - low)
        return image.map { layer ->
            layer.map { row ->
                row.map { pixel ->
                    ((pixel - low) * scaleFactor).coerceIn(0.0, 255.0).toInt()
                }.toTypedArray()
            }.toTypedArray()
        }.toTypedArray()
    }

    fun enhanceContrast(
            initArr: Array<Array<Array<Int>>>,
            leftPercentile: Double = 5.0,
            rightPercentile: Double = 95.0
    ): Array<Array<Array<Int>>> {
        val flattenedImage = initArr.flatten().flatMap { it.toList() }.toIntArray()
        val low = percentile(flattenedImage, leftPercentile)
        val high = percentile(flattenedImage, rightPercentile)
        return rescaleIntensity(initArr, Pair(low, high))
    }

    fun imageTo3DArray(image: BufferedImage): Array<Array<Array<Int>>> {
        val width = image.width
        val height = image.height
        val rgbArray: Array<Array<Array<Int>>> = Array(width) { Array(height) { Array(3) { 0 } } }

        for (x in 0 until width) {
            for (y in 0 until height) {
                val color = image.getRGB(x, y)
                rgbArray[x][y][0] = (color shr 16) and 0xFF // Red
                rgbArray[x][y][1] = (color shr 8) and 0xFF  // Green
                rgbArray[x][y][2] = color and 0xFF          // Blue
            }
        }

        return rgbArray
    }

    fun array3DToImage(rgbArray: Array<Array<Array<Int>>>): BufferedImage {
        val width = rgbArray.size
        val height = rgbArray[0].size
        // Alpha component is not really needed (but maybe interesting to try?)
        // So TYPE_INT_RGB - 24 bytes
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val r = rgbArray[x][y][0]
                val g = rgbArray[x][y][1]
                val b = rgbArray[x][y][2]
                val color = (r shl 16) or (g shl 8) or b
                image.setRGB(x, y, color)
            }
        }

        return image
    }

    fun array2DToImage(channel: Array<Array<Int>>): BufferedImage {

        val height = channel.size
        val width = channel[0].size
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (i in 0 until height) {
            for (j in 0 until width) {
                val gray = channel[i][j]
                val rgb = (gray shl 16) or (gray shl 8) or gray
                image.setRGB(j, i, rgb)
            }
        }

        return image
    }

    fun imageTo2DArray(image: BufferedImage): Array<Array<Int>> {
        val width = image.width
        val height = image.height
        val array = Array(height) { Array(width) { 0 } }

        for (i in 0 until height) {
            for (j in 0 until width) {
                val rgb = image.getRGB(j, i)
                val r = (rgb shr 16) and 0xFF
                array[i][j] = r
            }
        }
        return array
    }
}