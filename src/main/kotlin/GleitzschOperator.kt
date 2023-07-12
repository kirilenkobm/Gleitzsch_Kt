
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.math.roundToInt

const val DEFAULT_SHIFT_DENOMINATOR = 8.0
// Applies the Gleitzsch operator to images.

class GleitzschOperator(private val imageOperations: ImageOperations,
                        private val lameCompressor: LameCompressor)
{
    // Your Gleitzsch operation methods go here
    fun apply(image: BufferedImage, tmpDir: String, rgbShift: Int): BufferedImage {
        val origRgbArray = imageOperations.imageTo3DArray(image)
        val gleitzschedArr = doGleitzsch(origRgbArray, tmpDir, rgbShift)
        // the output has dramatically low contrast -> need to enhance it for aesthetic reasons
        val highContrastArr = imageOperations.enhanceContrast(gleitzschedArr)
        return imageOperations.array3DToImage(highContrastArr)
    }

    private fun buildGleitzschedArray(
            decompressedData: Array<DecompressedChannelData?>,
            gleitzschedArray: Array<Array<Array<Int>>>
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
                    gleitzschedArray[i][j][channelNum] = decompressedBytesArray[index].toInt() and 0xFF
                    index += 1
                }
            }
        }
    }


    // Create decompressedData structure from decompressed file
    private fun createDecompressedData(
        decompressedFile: File,
        byteArray: ByteArray,
        channel: Array<Array<Int>>
    ): DecompressedChannelData {
        val decompressedByteArray = Files.readAllBytes(decompressedFile.toPath())
        val byteArraySize = byteArray.size
        val channelIndices = channel.indices
        val channelZeroIndices = channel[0].indices

        return DecompressedChannelData(
            decompressedBytes = decompressedByteArray,
            byteArraySize = byteArraySize,
            channelIndices = channelIndices,
            channelZeroIndices = channelZeroIndices
        )
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

        runBlocking {
            val jobs = (0..2).map { channelNum ->
                async(Dispatchers.IO) {
                    // define paths to all intermediate files
                    val initialBytesFile = File("$tmpDir/${channelNum}_${UUID.randomUUID()}.bin")
                    val compressedFile = File("$tmpDir/${channelNum}_${UUID.randomUUID()}.mp3")
                    val decompressedFile = File("$tmpDir/${channelNum}_dec_${UUID.randomUUID()}.bin")

                    // extract and postprocess the channel
                    val channel = imageOperations.extractChannel(imageArr, channelNum)
                    val appliedShiftVal = rgbShift * channelNum
                    print("Applying shift: $appliedShiftVal")
                    val channelRgbShifted = imageOperations.addRgbShiftToArray(channel, appliedShiftVal)

                    // flat channel, convert to bytes and save to a bin (like "wav") file
                    // val flatChannel = imageOperations.flattenChannel(channel)
                    val flatChannel = imageOperations.flattenChannel(channelRgbShifted)
                    val byteArray = flatChannel.map { it.toByte() }.toByteArray()
                    Files.write(initialBytesFile.toPath(), byteArray)

                    // compress to mp3 and decompress back
                    lameCompressor.compressFile(initialBytesFile, compressedFile, channelNum)
                    lameCompressor.decompressFile(compressedFile, decompressedFile, channelNum)

                    // Store the decompressed data for later use
                    val result = createDecompressedData(decompressedFile, byteArray, channel)
                    decompressedData[channelNum] = result

                    Files.deleteIfExists(initialBytesFile.toPath())
                    Files.deleteIfExists(compressedFile.toPath())
                    Files.deleteIfExists(decompressedFile.toPath())
                }
            }
            jobs.awaitAll()
        }

        // Populate Gleitzched Array
        val gleitzschedArray = Array(imageArr.size) {
            Array(imageArr[0].size) {
                Array(imageArr[0][0].size) { 0 }
            }
        }
        buildGleitzschedArray(decompressedData, gleitzschedArray)

        val shift = shape.first - (shape.first / DEFAULT_SHIFT_DENOMINATOR).roundToInt()
        return imageOperations.shiftImage(gleitzschedArray, shift)
    }
}
