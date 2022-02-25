package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

fun main() {
    var input: String
    do {
        println("Task (hide, show, exit):")
        input = readLine()!!
        when (input) {
            Command.HIDE -> hide()
            Command.SHOW -> show()
            Command.EXIT -> exit()
            else -> println("Wrong task: $input")
        }
    } while (Command.EXIT != input)
}

fun exit() {
    println("Bye!")
}

fun show() {
    runCatching {
        println("Input image file:")
        val inputFile = File(readLine()!!)
        val image = ImageIO.read(inputFile)
        val uByteArray = findBitsInImage(image)
        val decodedMessage = uByteArray.toString()
        println(decodedMessage)
    }.onFailure {
        println("Can't read input file!")
    }
}

fun findBitsInImage(image: BufferedImage): ByteArray {
    val bits = mutableListOf<Int>()
    val bytes = mutableListOf<Byte>()
    val endBytes = listOf<Byte>(0, 0, 3)
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val color = Color(image.getRGB(x, y))
            val lastBit = color.rgb and 1
            bits.add(lastBit)
            if (bits.size == 8) {
                val byte = bits.joinToString("").toUByte(radix = 2).toInt().toByte()
                bytes.add(byte)
                if (bytes.containsAll(endBytes)) {
                    bytes.removeAll(endBytes)
                    return bytes.toByteArray()
                }
                bits.clear()
            }
        }
    }
    throw RuntimeException("Something is wrong")
}

fun hide() {
    runCatching {
        println("Input image file:")
        val inputFile = File(readLine()!!)
        println("Output image file:")
        val outputFile = File(readLine()!!)
        println("Message to hide:")
        val message = readLine()!!
        val byteArray = andEndingBytes(message.encodeToByteArray())
        val image = ImageIO.read(inputFile)
        val totalBits = image.width * image.height
        val storingBits = byteArray.size * 8
        if (totalBits < storingBits) {
            return println("The input image is not large enough to hold this message.")
        }
        println("Input Image: ${getRelativePathUnix(inputFile)}")
        println("Output Image: ${getRelativePathUnix(outputFile)}")
        changeColorOfImage(image, byteArray)
        saveImage(image, outputFile)
    }.onFailure {
        println("Can't read input file!")
    }
}

private fun andEndingBytes(byteArray: ByteArray): ByteArray {
    var byteArray1 = byteArray
    byteArray1 += 0
    byteArray1 += 0
    byteArray1 += 3
    return byteArray1
}

fun changeColorOfImage(image: BufferedImage, byteArray: ByteArray) {
    val allBits = byteArray.flatMap { getBits(it) }.toIntArray()
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val index = x + y
            if (allBits.size > index) {
                val bit = allBits[index]
                val color = Color(image.getRGB(x, y))
                color.rgb shr 1
                val newRgb = color.rgb.toString(2) + bit
                image.setRGB(x, y, newRgb.toInt(2))
            }
        }
    }
}

private fun getBits(byte: Byte): List<Int> {
    val masterByte = byte.toInt()
    val bits = mutableListOf<Int>()
    for (i in 0..7) {
        bits.add((masterByte shr i) and 1)
    }
    return bits.reversed()
}

fun saveImage(image: BufferedImage, file: File) {
    ImageIO.write(image, "png", file)
    println("Message saved in ${getRelativePathUnix(file)} image.")
}

private fun getRelativePathUnix(currentFile: File) = currentFile.path.replace("\\", "/")

class Command {
    companion object {
        const val HIDE = "hide"
        const val SHOW = "show"
        const val EXIT = "exit"
    }
}

