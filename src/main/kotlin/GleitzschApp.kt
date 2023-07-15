// Contains the main application logic and is the entry point of the program.


class GleitzschApp {
    private val imageIO = ImageIO()
    private val imagePreprocessor = ImagePreprocessor()
    private val imageOperations = ImageOperations()
    private val lameCompressor = LameCompressor()
    private val argsParser = GleitzschArgsParser()
    private val imageFilters = ImageFilters(imageOperations)
    private val gleitzschOperator = GleitzschOperator(
        imageOperations,
        lameCompressor,
        imageFilters
    )

    fun run(args: Array<String>) {
        val gleitzschArgs = argsParser.parse(args)

        val image = imageIO.loadImage(
                gleitzschArgs.inputImagePath
        )
        val preprocessedImage = imagePreprocessor.preprocessImage(
                image,
                gleitzschArgs
        )
        val resultImage = gleitzschOperator.apply(
                preprocessedImage,
                gleitzschArgs
        )
        imageIO.saveImage(
            resultImage,
            gleitzschArgs.outputImagePath
        )
    }
}
