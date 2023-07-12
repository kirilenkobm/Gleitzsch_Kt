// Holds decompressed channel data

data class DecompressedChannelData(
    val decompressedBytes: ByteArray,
    val byteArraySize: Int,
    val channelIndices: IntRange,
    val channelZeroIndices: IntRange
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DecompressedChannelData

        if (!decompressedBytes.contentEquals(other.decompressedBytes)) return false
        if (byteArraySize != other.byteArraySize) return false
        if (channelIndices != other.channelIndices) return false
        return channelZeroIndices == other.channelZeroIndices
    }

    override fun hashCode(): Int {
        var result = decompressedBytes.contentHashCode()
        result = 31 * result + byteArraySize
        result = 31 * result + channelIndices.hashCode()
        result = 31 * result + channelZeroIndices.hashCode()
        return result
    }
}
