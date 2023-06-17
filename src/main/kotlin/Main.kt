
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.system.exitProcess

const val DEFAULT_GAMMA = 10.0
const val DEFAULT_SHIFT_DENOMINATOR = 8.0

class ImageProcessor {
    // Potentially to rearrange together with ImageOperations
    // This class is rather to load/save image and do basic preprocessing
    fun loadImage(path: String, size: Int): BufferedImage {
        val origImage = ImageIO.read(File(path))
        val rescaledImage = rescaleImage(origImage, size)
        return adjustGamma(rescaledImage, DEFAULT_GAMMA)
    }

    fun saveImage(image: BufferedImage, path: String) {
        ImageIO.write(image, "jpg", File(path))
    }

    private fun rescaleImage(image: BufferedImage, size: Int): BufferedImage {
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
            gammaCorrectionLookup[i] = (255.0 * (i / 255.0).pow(1.0 / gamma)).toInt().toByte()
        }

        val width = image.width
        val height = image.height
        val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = image.getRGB(x, y)
                val a = pixel shr 24 and 0xFF
                val r = gammaCorrectionLookup[pixel shr 16 and 0xFF]
                val g = gammaCorrectionLookup[pixel shr 8 and 0xFF]
                val b = gammaCorrectionLookup[pixel and 0xFF]

                val alpha = a shl 24
                val red = (r.toInt() and 0xFF) shl 16
                val green = (g.toInt() and 0xFF) shl 8
                val blue = b.toInt() and 0xFF

                newImage.setRGB(x, y, alpha or red or green or blue)
            }
        }

        return newImage
    }
}


class LameCompressor(private val pathToLame: String) {

    fun compressFile(inputFile: File): File {
        val outputFile = File("${inputFile.parent}/${UUID.randomUUID()}.mp3")
        val command = StringBuilder()
            .append(pathToLame)
            .append(" -r")
            .append(" -s 8")
            .append(" -q 1")
            .append(" --highpass-width")
            .append(" --lowpass-width")
            .append(" --bitwidth 8")
            .append(" -b 8")
            .append(" -B 16")
            .append(" -m f")
            .append(" ${inputFile.absolutePath}")
            .append(" ${outputFile.absolutePath}")
            .toString()

        println("Started $command")
        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
        return outputFile
    }

    fun decompressFile(inputFile: File): File {
        val outputFile = File("${inputFile.parent}/dec_${UUID.randomUUID()}.bin")
        val command = "$pathToLame -S --decode --brief -x -t ${inputFile.absolutePath} ${outputFile.absolutePath}"
        println("Started $command")

        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
        return outputFile
    }
}


class ImageOperations {
    fun transformTo3DArray(image: BufferedImage): Array<Array<Array<Int>>> {
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

    fun arrayToImage(rgbArray: Array<Array<Array<Int>>>): BufferedImage {
        val width = rgbArray.size
        val height = rgbArray[0].size
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

    fun extractChannel(imageArr: Array<Array<Array<Int>>>, channel: Int): Array<Array<Int>> {
        val width = imageArr.size
        val height = imageArr[0].size

        return Array(width) { i ->
            Array(height) { j ->
                imageArr[i][j][channel]
            }
        }
    }

    fun flattenChannel(channel: Array<Array<Int>>): Array<Int> {
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

    fun percentile(arr: IntArray, percentile: Double): Int {
        val index = (percentile / 100) * arr.size
        return arr.sorted()[index.toInt()]
    }

    fun rescaleIntensity(image: Array<Array<Array<Int>>>, inRange: Pair<Int, Int>): Array<Array<Array<Int>>>
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
}


class GleitzschOperator(private val imageOperations: ImageOperations,
                        private val lameCompressor: LameCompressor)
{
    // Your Gleitzsch operation methods go here
    fun apply(image: BufferedImage, tmpDir: String, rgbShift: Int): BufferedImage {
        val origRgbArray = imageOperations.transformTo3DArray(image)
        val gleitzschedArr = doGleitzsch(origRgbArray, tmpDir, rgbShift)
        // Flatten the 3D array into 1D array for percentile calculations
        val flattenedGlitchedImage = gleitzschedArr.flatten().flatMap { it.toList() }.toIntArray()
        val low = imageOperations.percentile(flattenedGlitchedImage, 5.0)
        val high = imageOperations.percentile(flattenedGlitchedImage, 95.0)
        val highContrastArr = imageOperations.rescaleIntensity(gleitzschedArr, Pair(low, high))
        return imageOperations.arrayToImage(highContrastArr)
    }

    private fun doGleitzsch(
        imageArr: Array<Array<Array<Int>>>,
        tmpDir: String,
        rgbShift: Int
    ): Array<Array<Array<Int>>>
    {
        val shape = Triple(imageArr.size, imageArr[0].size, imageArr[0][0].size)
        println("Original array shape: $shape")

        val glitzschedArray = Array(imageArr.size) {
            Array(imageArr[0].size) {
                Array(imageArr[0][0].size) { 0 }
            }
        }

        for (channelNum in 0..2) {
            val channel = imageOperations.extractChannel(imageArr, channelNum)
            val appliedShiftVal = rgbShift * channelNum
            println("$channelNum channel shape: ${channel.size}x${channel[0].size}")
            val flatChannel = imageOperations.flattenChannel(channel)
            val byteArray = flatChannel.map { it.toByte() }.toByteArray()
            val tempFile = Paths.get("$tmpDir/${channelNum}_${UUID.randomUUID()}.bin")
                .toFile()
            val origSize = byteArray.size
            Files.write(tempFile.toPath(), byteArray)

            val compressedFile = lameCompressor.compressFile(tempFile)
            val decompressedFile = lameCompressor.decompressFile(compressedFile)
            val decompressedByteArray = Files.readAllBytes(decompressedFile.toPath())
            val newSize = decompressedByteArray.size
            println(
                "Orig: ${origSize}; " +
                "lame-ed array size: ${newSize}; " +
                "approx ${(newSize / origSize)} bigger"
            )

            val decompressedBytesArray = decompressedByteArray
                .toList()
                .chunked(2)
                .take(byteArray.size)
                .map { it[0] }
                .toByteArray()

            var index = 0
            for (j in channel[0].indices) {
                for (i in channel.indices) {
                    glitzschedArray[i][j][channelNum] = decompressedBytesArray[index].toInt() and 0xFF
                    index += 1
                }
            }
            Files.deleteIfExists(tempFile.toPath())
            Files.deleteIfExists(compressedFile.toPath())
            Files.deleteIfExists(decompressedFile.toPath())
        }

        val shift = shape.first - (shape.first / DEFAULT_SHIFT_DENOMINATOR).roundToInt()
        return imageOperations.shiftImage(glitzschedArray, shift)
    }
}


class Application {
    private val pathToLame = getPathToLame()
    private val processor = ImageProcessor()
    private val imageOperations = ImageOperations()
    private val lameCompressor = LameCompressor(pathToLame)
    private val gleitzschOperator = GleitzschOperator(imageOperations, lameCompressor)

    fun run(args: Array<String>) {
        val parser = ArgParser("GleitzschOperator")
        val inputImagePath by parser.argument(ArgType.String, description = "Path to the input image")
        val outputImagePath by parser.argument(ArgType.String, description = "Path to the output image")
        val imageSize by parser.option(ArgType.Int, description = "Size of the image")
            .default(1024)
        val tempDir by parser.option(ArgType.String,
            description = "Path to the temp directory")
            .default("tmpDir")
        val rgbShift by parser.option(ArgType.Int, description = "RGB shift to be applied")
            .default(8)

        parser.parse(args)

        val image = processor.loadImage(inputImagePath, imageSize)
        val result = gleitzschOperator.apply(image, tempDir, rgbShift)
        processor.saveImage(result, outputImagePath)
    }

    private fun getPathToLame(): String {
        val runtime = Runtime.getRuntime()
        val whichProcess = runtime.exec(arrayOf("bash", "-c", "which lame"))
        whichProcess.waitFor()
        val exitValue = whichProcess.exitValue()

        return if (exitValue == 0) {
            whichProcess.inputStream.reader().use { it.readText().trim() }
        } else {
            // write err message
            exitProcess(1)
        }
    }
}


fun main(args: Array<String>) {
    Application().run(args)
}
