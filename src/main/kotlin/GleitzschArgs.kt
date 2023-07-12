// Handle command line arguments
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default


const val DEFAULT_GAMMA = 10.0

data class GleitzschArgs(
    val inputImagePath: String,
    val outputImagePath: String,
    val imageSize: Int,
    val tempDir: String,
    val rgbShift: Int,
    val gamma: Double
)

class GleitzschArgsParser {
    fun parse(args: Array<String>): GleitzschArgs {
        val parser = ArgParser("Gleitzsch")

        val inputImagePath by parser.argument(ArgType.String, description = "Path to the input image")
        val outputImagePath by parser.argument(ArgType.String, description = "Path to the output image")
        val imageSize by parser.option(
                ArgType.Int,
                description = "Size of the processed image (long size)"
        ).default(1024)
        val tempDir by parser.option(
                ArgType.String,
                description = "Path to the temp directory"
        ).default("tempDir")

        val rgbShift by parser.option(ArgType.Int, description = "RGB shift to be applied").default(8)
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