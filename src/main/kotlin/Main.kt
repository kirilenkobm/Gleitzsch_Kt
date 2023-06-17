
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
import kotlin.system.exitProcess

class ImageProcessor {
    fun loadImage(path: String, size: Int): BufferedImage {
        val origImage = ImageIO.read(File(path))
        return rescaleImage(origImage, size)
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
        g.drawImage(image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null)
        g.dispose()

        return scaledImage
    }
}

class GleitzschOperator {
    private var pathToLame: String? = null

    init {
        pathToLame = getPathToLame()
        if (pathToLame == null) {
            exitProcess(1)
        }
    }

    // Your Gleitzsch operation methods go here
    fun apply(image: BufferedImage, tmpDir: String): BufferedImage {
        // val origRgbArray = transformTo3DArray(image)
        // val scaledAdjImage = adjustGamma(scaledImage, 2.0)
        val origRgbArray = transformTo3DArray(image)
        val gleitzschedArr = doGleitzsch(origRgbArray, tmpDir)
        return arrayToImage(gleitzschedArr)
    }

    private fun getPathToLame(): String? {
        val runtime = Runtime.getRuntime()
        val whichProcess = runtime.exec(arrayOf("bash", "-c", "which lame"))
        whichProcess.waitFor()
        val exitValue = whichProcess.exitValue()

        return if (exitValue == 0) {
            whichProcess.inputStream.reader().use { it.readText().trim() }
        } else {
            null
        }
    }

    private fun doGleitzsch(imageArr: Array<Array<Array<Int>>>, tmpDir: String): Array<Array<Array<Int>>> {
        val flatArray = imageArr.flatMap { arrayOfArrays -> arrayOfArrays.flatMap { it.toList() } }
        println(flatArray.take(10))
        val byteArray = flatArray.map { it.toByte() }.toByteArray()
        val randomFileName = "${UUID.randomUUID()}.bin"
        val tempFile = Paths.get("$tmpDir/$randomFileName").toFile()
        Files.write(tempFile.toPath(), byteArray)
        println("Original bytesize: ${byteArray.size}")

        val compressedFile = compressFile(tempFile)
        println("Compressed file path: ${compressedFile.absolutePath}")

        val decompressedFile = decompressFile(compressedFile)
        println("Decompressed file path: ${decompressedFile.absolutePath}")

        val decompressedByteArray = Files.readAllBytes(decompressedFile.toPath())
        println("Decompressed bytesize: ${decompressedByteArray.size}")

        val decompressedBytesArray = decompressedByteArray.toList().chunked(2).take(byteArray.size).map { it[0] }.toByteArray()

        val reshapedArray = Array(imageArr.size) {
            Array(imageArr[0].size) {
                Array(imageArr[0][0].size) { 0 }
            }
        }

        var index = 0
        for (i in reshapedArray.indices) {
            for (j in reshapedArray[0].indices) {
                for (k in reshapedArray[0][0].indices) {
                    reshapedArray[i][j][k] = decompressedBytesArray[index].toInt()
                    index++
                }
            }
        }

        return reshapedArray
    }

    private fun compressFile(inputFile: File): File {
        val outputFile = File("${inputFile.parent}/${UUID.randomUUID()}.mp3")
        val command = "$pathToLame -r --unsigned -s 8 -q 2 --bitwidth 8 -m m ${inputFile.absolutePath} ${outputFile.absolutePath}"
        println("Started $command")
        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
        return outputFile
    }

    private fun decompressFile(inputFile: File): File {
        val outputFile = File("${inputFile.parent}/dec_${UUID.randomUUID()}.bin")
        val command = "$pathToLame --decode -x -t ${inputFile.absolutePath} ${outputFile.absolutePath}"
        println("Started $command")

        val process = ProcessBuilder(*command.split(" ").toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        process.waitFor()
        return outputFile
    }


    private fun arrayToImage(rgbArray: Array<Array<Array<Int>>>): BufferedImage {
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

    private fun transformTo3DArray(image: BufferedImage): Array<Array<Array<Int>>> {
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
}


fun adjustGamma(image: BufferedImage, gamma: Double): BufferedImage {
    val gammaCorrectionLookup = ByteArray(256)

    for (i in 0 until gammaCorrectionLookup.size) {
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
            newImage.setRGB(x, y, (a shl 24) or ((r.toInt() and 0xFF) shl 16) or ((g.toInt() and 0xFF) shl 8) or (b.toInt() and 0xFF))
        }
    }

    return newImage
}


class Application {
    private val processor = ImageProcessor()
    private val operator = GleitzschOperator()

    fun run(args: Array<String>) {
        val parser = ArgParser("GleitzschOperator")
        val inputImagePath by parser.argument(ArgType.String, description = "Path to the input image")
        val outputImagePath by parser.argument(ArgType.String, description = "Path to the output image")
        val imageSize by parser.option(ArgType.Int, description = "Size of the image").default(1024)
        val tempDir by parser.option(ArgType.String, description = "Path to the temp directory").default("tmpDir")
        val rgbShift by parser.option(ArgType.Int, description = "RGB shift to be applied").default(8)

        parser.parse(args)
        // Your application logic goes here

        val image = processor.loadImage(inputImagePath, imageSize)
        val result = operator.apply(image, tempDir)
        processor.saveImage(result, outputImagePath)
    }
}


fun main(args: Array<String>) {
    Application().run(args)
}