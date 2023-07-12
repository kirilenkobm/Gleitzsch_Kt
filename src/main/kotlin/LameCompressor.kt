// Handles the compression and decompression of files using lame.
import java.io.File
import java.util.*
import kotlin.system.exitProcess


class LameCompressor() {
    private val pathToLame: String = getPathToLame()

    fun compressFile(inputFile: File, channelNum: Int): File {
        // call lame on an input file containing raw byte array -> return path to mp3 file
        val outputFile = File("${inputFile.parent}/${channelNum}_${UUID.randomUUID()}.mp3")
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
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        process.waitFor()
        return outputFile
    }

    fun decompressFile(inputFile: File, channelNum: Int): File {
        // decompress given mp3 file - get sort of uncompressed "wav"
        val outputFile = File("${inputFile.parent}/${channelNum}_dec_${UUID.randomUUID()}.bin")
        val command = "$pathToLame -S --decode --brief -x -t ${inputFile.absolutePath} ${outputFile.absolutePath}"
        println("Started $command")

        val process = ProcessBuilder(*command.split(" ").toTypedArray())
//            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
//            .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        process.waitFor()
        return outputFile
    }

    private fun getPathToLame(): String {
        val runtime = Runtime.getRuntime()
        val whichProcess = runtime.exec(arrayOf("bash", "-c", "which lame"))
        whichProcess.waitFor()
        val exitValue = whichProcess.exitValue()

        return if (exitValue == 0) {
            // ret path to lame
            whichProcess.inputStream.reader().use { it.readText().trim() }
        } else {
            // TODO: write err message
            // something like lame is required, pls install it
            exitProcess(1)
        }
    }
}
