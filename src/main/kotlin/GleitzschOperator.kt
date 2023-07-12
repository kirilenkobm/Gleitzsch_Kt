import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.math.roundToInt

const val DEFAULT_SHIFT_DENOMINATOR = 8.0
// Applies the Gleitzsch operator to images.

class GleitzschOperator(private val imageOperations: ImageOperations,
                        private val lameCompressor: LameCompressor)
{
    // Your Gleitzsch operation methods go here
    fun apply(image: BufferedImage, tmpDir: String, rgbShift: Int): BufferedImage {
        val origRgbArray = imageOperations.transformTo3DArray(image)
        val gleitzschedArr = doGleitzsch(origRgbArray, tmpDir, rgbShift)
        // the output has dramatically low contrast -> need to enhance it for aesthetic reasons
        val highContrastArr = imageOperations.enhanceContrast(gleitzschedArr)
        return imageOperations.arrayToImage(highContrastArr)
    }

    private fun buildGleitzschedArray(
            decompressedData: Array<DecompressedChannelData?>,
            glitzschedArray: Array<Array<Array<Int>>>
    ) {
        for (channelNum in 0..2) {
            val data = decompressedData[channelNum]!!
            val decompressedBytesArray = data.decompressedBytes
                    .toList()
                    .chunked(2)
                    .take(data.byteArraySize)
                    .map { it[0] }
                    .toByteArray()

            var index = 0
            for (j in data.channelZeroIndices) {
                for (i in data.channelIndices) {
                    glitzschedArray[i][j][channelNum] = decompressedBytesArray[index].toInt() and 0xFF
                    index += 1
                }
            }
        }
    }

    fun addRgbShiftToArray(
            channel: Array<Array<Int>>,
            appliedShiftVal: Int
    ): Array<Array<Int>>
    {
        // TODO: fix this func
        // If the appliedShiftVal is zero, return the channel unchanged
        if (appliedShiftVal == 0) {
            return channel
        }
        println("Channel shape: ${channel.size}x${channel[0].size} ")

        // Otherwise, create a BufferedImage from the channel
        val height = channel.size
        val width = channel[0].size
        val image = imageOperations.array2DToImage(channel)

        // Rescale the image, adding + appliedShiftVal to each side
        val newWidth = width + 2 * appliedShiftVal
        val newHeight = height + 2 * appliedShiftVal
        val rescaledImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
        val g2d = rescaledImage.createGraphics()
        g2d.drawImage(image, appliedShiftVal, appliedShiftVal, width, height, null)
        g2d.dispose()

        // Trim the central part of the image, removing the borders
        val croppedImage = rescaledImage.getSubimage(appliedShiftVal, appliedShiftVal, width, height)

        // Convert the BufferedImage back into a 2D array
        // Convert the BufferedImage back into a 2D array
        val result = Array(height) { Array(width) { 0 } } // swap width and height
        for (i in 0 until height) {                       // iterate over height first
            for (j in 0 until width) {                    // then over width
                val color = Color(croppedImage.getRGB(j, i)) // swap i and j
                result[i][j] = color.red
            }
        }
        println("Result shape: ${result.size}x${result[0].size} ")


        return result
    }


    private fun doGleitzsch(
            imageArr: Array<Array<Array<Int>>>,
            tmpDir: String,
            rgbShift: Int
    ): Array<Array<Array<Int>>>
    {
        val shape = Triple(imageArr.size, imageArr[0].size, imageArr[0][0].size)
        println("Original array shape: $shape")
        val decompressedData = arrayOfNulls<DecompressedChannelData>(3)

        val glitzschedArray = Array(imageArr.size) {
            Array(imageArr[0].size) {
                Array(imageArr[0][0].size) { 0 }
            }
        }

        runBlocking {
            val jobs = (0..2).map { channelNum ->
                async(Dispatchers.IO) {
                    val channel = imageOperations.extractChannel(imageArr, channelNum)
                    val appliedShiftVal = rgbShift * channelNum
                    //  val channelRgbShifted = addRgbShiftToArray(channel, appliedShiftVal)

                    // println("$channelNum channel shape: ${channelRgbShifted.size}x${channelRgbShifted[0].size}")
                    val flatChannel = imageOperations.flattenChannel(channel)
                    val byteArray = flatChannel.map { it.toByte() }.toByteArray()
                    val tempFile = Paths.get("$tmpDir/${channelNum}_${UUID.randomUUID()}.bin")
                            .toFile()
                    Files.write(tempFile.toPath(), byteArray)

                    val compressedFile = lameCompressor.compressFile(tempFile, channelNum)
                    val decompressedFile = lameCompressor.decompressFile(compressedFile, channelNum)

                    val decompressedByteArray = Files.readAllBytes(decompressedFile.toPath())
                    val byteArraySize = byteArray.size
                    val channelIndices = channel.indices
                    val channelZeroIndices = channel[0].indices
//                    val origSize = byteArray.size
//                    val newSize = decompressedByteArray.size
//                    println("Orig: ${origSize}; "lame-ed : ${newSize}; "approx ${(newSize / origSize)} bigger")

                    // Store the decompressed data for later use
                    val result = DecompressedChannelData(
                            decompressedBytes = decompressedByteArray,
                            byteArraySize = byteArraySize,
                            channelIndices = channelIndices,
                            channelZeroIndices = channelZeroIndices
                    )
                    decompressedData[channelNum] = result

                    Files.deleteIfExists(tempFile.toPath())
                    Files.deleteIfExists(compressedFile.toPath())
                    Files.deleteIfExists(decompressedFile.toPath())
                }
            }
            jobs.awaitAll()
        }

        // Populate Gleitzched Array
        buildGleitzschedArray(decompressedData, glitzschedArray)

        val shift = shape.first - (shape.first / DEFAULT_SHIFT_DENOMINATOR).roundToInt()
        return imageOperations.shiftImage(glitzschedArray, shift)
    }
}
