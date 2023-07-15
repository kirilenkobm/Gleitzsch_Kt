import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage

// collection of artistic filters

class ImageFilters(private val imageOperations: ImageOperations) {
    // add glitter
    // add streaks
    // and other artistic filters
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
