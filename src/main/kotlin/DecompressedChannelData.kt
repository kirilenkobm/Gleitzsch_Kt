// Holds decompressed channel data

data class DecompressedChannelData(
        val decompressedBytes: ByteArray,
        val byteArraySize: Int,
        val channelIndices: IntRange,
        val channelZeroIndices: IntRange
)
