import java.awt.image.BufferedImage

// collection of artistic filters

class ImageFilters(private val imageOperations: ImageOperations) {
    // add glitter
    // add streaks
    // and other artistic filters
    fun applyRgbShiftChannel(image: BufferedImage, degree: Int): BufferedImage{
        if (degree == 0) {
            // applying the filter will have no effect -> just return the image
            return image
        }

        val imageArray = imageOperations.imageTo3DArray(image)
        val height = imageArray.size
        val width = imageArray[0].size
        val channels = imageArray[0][0].size
        val newImageArray: Array<Array<Array<Int>>> = Array(height) { Array(width) { Array(channels) {0} } }

        for (channelNum in 0..2) {
            val channel = imageOperations.extractChannel(imageArray, channelNum)
            val appliedShiftVal = degree * channelNum
            // 2D array of the same shape
            val channelRgbShifted = imageOperations.addRgbShiftToArray(channel, appliedShiftVal)

            for (i in 0 until height) {
                for (j in 0 until width) {
                    newImageArray[i][j][channelNum] = channelRgbShifted[i][j]
                }
            }
        }
        return imageOperations.array3DToImage(newImageArray)
    }
}
