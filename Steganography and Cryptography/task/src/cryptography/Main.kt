package cryptography

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.security.MessageDigest
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
        val byteArray = findBitsInImage(image)
        val decodedMessage = byteArray.toString(Charsets.UTF_8)
        println(decodedMessage)
    }.onFailure {
        println("Can't read input file!")
    }
}

fun findBitsInImage(image: BufferedImage): ByteArray {
    val bits = mutableListOf<Int>()
    val bytes = mutableListOf<Byte>()
    val endBytes = listOf<Byte>(0, 0, 3)
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
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
        val inputImage = ImageIO.read(inputFile)
        val outputImage = BufferedImage(inputImage.width, inputImage.height, BufferedImage.TYPE_INT_RGB)
        val totalBits = outputImage.width * outputImage.height
        val storingBits = byteArray.size * 8
        if (totalBits < storingBits) {
            return println("The input image is not large enough to hold this message.")
        }
//        println("Input Image: ${getRelativePathUnix(inputFile)}")
//        println("Output Image: ${getRelativePathUnix(outputFile)}")
        setColorOfNewImage(inputImage, outputImage, byteArray)
        saveImage(outputImage, outputFile)
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

fun setColorOfNewImage(original: BufferedImage, newImage: BufferedImage, byteArray: ByteArray) {
    val allBits = byteArray.flatMap { getBits(it) }.toIntArray()
    var traversedBits = 0
    for (y in 0 until newImage.height) {
        for (x in 0 until newImage.width) {
            val color = Color(original.getRGB(x, y))
            if (traversedBits < allBits.size) {
                val bit = allBits[traversedBits++]
                val colorWithoutLastBit = color.rgb.toString(2).dropLast(1)
                val newRgb = (colorWithoutLastBit + bit).toInt(2)
                newImage.setRGB(x, y, newRgb)
            } else {
                newImage.setRGB(x, y, color.rgb)
            }
        }
    }
}

private fun getBits(byte: Byte): List<Int> {
    val masterByte = byte.toInt()
    val bits = mutableListOf<Int>()
    for (i in 0..7) {
        bits += (masterByte shr i) and 1
    }
    return bits.reversed()
}

fun saveImage(image: BufferedImage, file: File) {
    println(imageHash(image))
    ImageIO.write(image, "png", file)
    println("Message saved in ${getRelativePathUnix(file)} image.")
}

private fun imageHash(inputImage: BufferedImage): String {
    val imageByteArray = ByteArray(3 * inputImage.width * inputImage.height)
    var index = 0
    for (y in 0 until inputImage.height) {
        for (x in 0 until inputImage.width) {
            val color = Color(inputImage.getRGB(x, y))
            imageByteArray[index] = color.red.toByte()
            index++
            imageByteArray[index] = color.green.toByte()
            index++
            imageByteArray[index] = color.blue.toByte()
            index++
        }
    }
    val md = MessageDigest.getInstance("SHA-1")
    md.update(imageByteArray)
    return md.digest().joinToString("") { "%02x".format(it) }
}

private fun getRelativePathUnix(currentFile: File) = currentFile.path.replace("\\", "/")

class Command {
    companion object {
        const val HIDE = "hide"
        const val SHOW = "show"
        const val EXIT = "exit"
    }
}

