import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// Handles all image I/O operations.

class ImageIO {
    fun loadImage(path: String): BufferedImage {
        // TODO: add err handling
        // if image is grayscale (1D - will Kotlin do 3D array?)
        return ImageIO.read(File(path))
    }

    fun saveImage(image: BufferedImage, path: String) {
        ImageIO.write(image, "jpg", File(path))
    }
}
