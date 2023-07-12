
import java.awt.Image
import java.awt.image.BufferedImage
import kotlin.math.pow


// Conducts preprocessing tasks such as adjusting gamma, rescaling images, and other preprocessing operations.

class ImagePreprocessor {
    private fun rescaleImage(image: BufferedImage, size: Int): BufferedImage {
        // where size -> the desired longest dimension
        val scaleFactor: Double = size.toDouble() / maxOf(image.width, image.height)
        val newWidth = (image.width * scaleFactor).toInt()
        val newHeight = (image.height * scaleFactor).toInt()

        val scaledImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        val g = scaledImage.createGraphics()
        g.drawImage(image.getScaledInstance(newWidth,
                                            newHeight,
                                            Image.SCALE_SMOOTH),
                                            0,
                                            0,
                                            null)
        g.dispose()

        return scaledImage
    }

    private fun adjustGamma(image: BufferedImage, gamma: Double): BufferedImage {
        val gammaCorrectionLookup = ByteArray(256)

        for (i in gammaCorrectionLookup.indices) {
            // for each possible byte -> get gamma-corrected value
            // original_val ^ (1 / gamma)
            gammaCorrectionLookup[i] = (255.0 * (i / 255.0).pow(1.0 / gamma)).toInt().toByte()
        }

        val width = image.width
        val height = image.height
        val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until height) {
            for (x in 0 until width) {
                // iterate over each pixel
                val pixel = image.getRGB(x, y)
                // extract ARGB values from pixel int (32-bits integer)
                val a = pixel shr 24 and 0xFF
                // RGB values are to be replaced with corrected ones according to
                // the gamma lookup table
                val r = gammaCorrectionLookup[pixel shr 16 and 0xFF]
                val g = gammaCorrectionLookup[pixel shr 8 and 0xFF]
                val b = gammaCorrectionLookup[pixel and 0xFF]

                // restore the pixel
                val alpha = a shl 24
                val red = (r.toInt() and 0xFF) shl 16
                val green = (g.toInt() and 0xFF) shl 8
                val blue = b.toInt() and 0xFF

                newImage.setRGB(x, y, alpha or red or green or blue)
            }
        }

        return newImage
    }

    fun preprocessImage(image: BufferedImage, size: Int, gamma: Double): BufferedImage {
        val rescaledImage = rescaleImage(image, size)
        return adjustGamma(rescaledImage, gamma)
    }
}
