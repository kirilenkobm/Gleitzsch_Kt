// Handle command line arguments
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

const val DEFAULT_IMAGE_SIZE = 1024
const val DEFAULT_GAMMA = 10.0
const val DEFAULT_RGB_SHIFT = 2
const val DEFAULT_CONTRAST_LEFT_PERCENTILE = 4.0
const val DEFAULT_CONTRAST_RIGHT_PERCENTILE = 96.0


data class GleitzschArgs(
    val inputImagePath: String,
    val outputImagePath: String,
    val imageSize: Int,
    val tempDir: String,
    val rgbShift: Int,
    val gamma: Double,
    val leftPercentile: Double,
    val rightPercentile: Double
) {
    fun printAttributes() {
        println("Gleitzsch run options:")
        println("inputImagePath: $inputImagePath")
        println("outputImagePath: $outputImagePath")
        println("imageSize: $imageSize")
        println("tempDir: $tempDir")
        println("rgbShift: $rgbShift")
        println("gamma: $gamma")
        println("leftPercentile: $leftPercentile")
        println("rightPercentile: $rightPercentile")
    }
}

fun createTempDir(): Path {
    val prefix = "tmp_gleitzsch_${UUID.randomUUID()}_"
    val tempDir = Files.createTempDirectory(prefix)
    tempDir.toFile().deleteOnExit()
    return tempDir
}


class GleitzschArgsParser {
    fun parse(args: Array<String>): GleitzschArgs {
        val parser = ArgParser("Gleitzsch")

        val inputImagePath by parser.argument(ArgType.String, description = "Path to the input image")
        val outputImagePath by parser.argument(ArgType.String, description = "Path to the output image")
        val imageSize by parser.option(
            ArgType.Int,
            description = "Size of the processed image (long dimension, default $DEFAULT_IMAGE_SIZE)"
        ).default(DEFAULT_IMAGE_SIZE)

        val defaultTempDir = createTempDir().toFile().absolutePath
        val tempDirArg by parser.option(
            ArgType.String,
            description = """
            Define temp directory. If provided by you -
            the intermediate files will not be deleted
            """
        ).default(defaultTempDir)

        val rgbShift by parser.option(
            ArgType.Int,
            description = "RGB shift to be applied (default $DEFAULT_RGB_SHIFT)"
        ).default(DEFAULT_RGB_SHIFT)

        val gammaParam by parser.option(
            ArgType.Double,
            description = "Preprocessing gamma adjustment (default $DEFAULT_GAMMA)"
        ).default(DEFAULT_GAMMA)

        val leftPercentile by parser.option(
            ArgType.Double,
            description = """
                Left intensity percentile for contrast enhancement (default $DEFAULT_CONTRAST_LEFT_PERCENTILE)
                """
        ).default(DEFAULT_CONTRAST_LEFT_PERCENTILE)

        val rightPercentile by parser.option(
            ArgType.Double,
            description = """
                Right intensity percentile for contrast enhancement (default $DEFAULT_CONTRAST_RIGHT_PERCENTILE)
                """
        ).default(DEFAULT_CONTRAST_RIGHT_PERCENTILE)

        parser.parse(args)

        val args = GleitzschArgs(
            inputImagePath,
            outputImagePath,
            imageSize,
            tempDirArg,
            rgbShift,
            gammaParam,
            leftPercentile,
            rightPercentile
        )
        args.printAttributes()
        return args
    }
}
