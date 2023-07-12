// Handle command line arguments
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

const val DEFAULT_IMAGE_SIZE = 1024
const val DEFAULT_GAMMA = 10.0
const val DEFAULT_RGB_SHIFT = 4


data class GleitzschArgs(
    val inputImagePath: String,
    val outputImagePath: String,
    val imageSize: Int,
    val tempDir: String,
    val rgbShift: Int,
    val gamma: Double
)

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
                description = "Size of the processed image (long size)"
        ).default(DEFAULT_IMAGE_SIZE)

        val tempDirArg by parser.option(
                ArgType.String,
                description = """
                Define temp directory. If provided by you -
                the intermediate files will not be deleted
                """
        )
        val tempDir = tempDirArg ?: createTempDir().toFile().absolutePath

        val rgbShift by parser.option(ArgType.Int, description = "RGB shift to be applied").default(DEFAULT_RGB_SHIFT)
        val gammaParam by parser.option(
                ArgType.Double,
                description = "Preprocessing gamma adjustment"
        ).default(DEFAULT_GAMMA)
        parser.parse(args)

        return GleitzschArgs(
                inputImagePath,
                outputImagePath,
                imageSize,
                tempDir,
                rgbShift,
                gammaParam,
        )
    }
}