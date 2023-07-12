// Contains the main application logic and is the entry point of the program.


class GleitzschApp {
    private val imageIO = ImageIO()
    private val imagePreprocessor = ImagePreprocessor()
    private val imageOperations = ImageOperations()
    private val lameCompressor = LameCompressor()
    private val gleitzschOperator = GleitzschOperator(imageOperations, lameCompressor)
    private val argsParser = GleitzschArgsParser()

    fun run(args: Array<String>) {
        val gleitzschArgs = argsParser.parse(args)

        val image = imageIO.loadImage(
                gleitzschArgs.inputImagePath,
                gleitzschArgs.imageSize
        )
        val preprocessedImage = imagePreprocessor.preprocessImage(
                image,
                gleitzschArgs.imageSize,
                gleitzschArgs.gamma
        )
        val resultImage = gleitzschOperator.apply(
                preprocessedImage,
                gleitzschArgs.tempDir,
                gleitzschArgs.rgbShift
        )
        imageIO.saveImage(resultImage, gleitzschArgs.outputImagePath) // Use imageIO to save the result
    }
}
