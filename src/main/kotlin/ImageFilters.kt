// collection of artistic filters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage

class ImageFilters(private val imageOperations: ImageOperations) {
    // add glitter
    // add streaks
    // and other artistic filters

    // TODO: coeff as command line argument
    fun interlace(image: BufferedImage, coeff: Int = 2): BufferedImage {
        val width = image.width
        val height = image.height
        val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val darkeningFactor = 1.35f

        for (y in 0 until height) {
            for (x in 0 until width) {
                val color = Color(image.getRGB(x, y))
                val factor = if (y / coeff % 2 == 0) 1 / darkeningFactor else 1.0f

                val r = (color.red * factor).coerceIn(0f, 255f).toInt()
                val g = (color.green * factor).coerceIn(0f, 255f).toInt()
                val b = (color.blue * factor).coerceIn(0f, 255f).toInt()

                newImage.setRGB(x, y, Color(r, g, b).rgb)
            }
        }

        return newImage
    }


    fun applyRgbShiftChannel(image: BufferedImage, degree: Int): BufferedImage = runBlocking{
        if (degree == 0) {
            // applying the filter will have no effect -> just return the image
            return@runBlocking image
        }

        val imageArray = imageOperations.imageTo3DArray(image)
        val height = imageArray.size
        val width = imageArray[0].size
        val channels = imageArray[0][0].size

        val deferredChannels = List(channels) {channelNum ->
            async(Dispatchers.Default) {
                val channel = imageOperations.extractChannel(imageArray, channelNum)
                val appliedShiftVal = degree * channelNum
                imageOperations.addRgbShiftToArray(channel, appliedShiftVal)
            }
        }

        val newImageArray: Array<Array<Array<Int>>> = Array(height) { Array(width) { Array(channels) {0} } }

        deferredChannels.forEachIndexed { channelNum, deferred ->
            val channel = deferred.await()
            for (i in 0 until height) {
                for (j in 0 until width) {
                    newImageArray[i][j][channelNum] = channel[i][j]
                }
            }
        }

        return@runBlocking imageOperations.array3DToImage(newImageArray)
    }
}
