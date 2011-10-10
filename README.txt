Introduction
============

Stego.jar is a very simple command-line tool for steganography. *.java is the
source for the jar. The usage follows::

    java -jar Stego.jar [e|d|a]
        e : encrypt a message into an image
        d : decrypt a message from an image
        a : perform analysis on an image

Stego.jar hides messages in pseudorandom bits in images. For more information on
how it works, the comments in StegImage.java might suffice, or you might want to
read this step-by-step implementation guide that I've put together below.


Warning!
========

If you read the source code, be prepared for bit-twiddling.


What is Steganography?
======================

Steganography is the art of sending a hidden message without letting anybody
except those involved that the message even exists. This is different from
cryptography; in cryptography, the presence of an encrypted message is obvious.

The simplest way to send a steganographic message is to do so in an image. Each
of the message bits is placed into the least significant bit of a pixel's color
value. The difference is not noticeable by the human eye, but if you know that
the message is there you can read it.


Implementation Details
======================

This section attempts to explain the differences between this implementation of
steganography and the simplest possible version.

The very first thing that should be done upon input of the message is
encryption. In Stego.jar, encryption is performed using a cipher called DES.
Being cryptography, DES is beyond the scope of this paper. Because the message
will always be encrypted during writing and reading, we will hereafter refer to
the encrypted version of the message as ”the message” for ease of reading.
The immediate and most intuitive way to write the message is to break it into bits, and then write one bit to each pixel of the image in order. For example,
if our message was 0100101, we would change the least significant bit of each of
the first 7 pixels from the top left of the image to match the ones in our
message. Though this technically works, it is fraught with weaknesses.
One major difference between this implementation of steganography and others
that can be found on the Internet is that this implementation writes to the two
least significant bits of each color value. The main reason for doing this is
size. A message that changes a small amount of the pixels in its cover is far less likely to be discovered. The tradeoff in this is that the pixels that are
modified will be slightly easier for the human eye to recognize.
When we change the two least significant bits of each color value, we can
calculate the largest distance that a pixel can change from its original value.
Each color value can change by a maximum of 3 values, meaning that the maximum
change would be 9 values, which is still nearly indistinguishable by the human
eye. In an image with a large bit depth, it is even harder to notice.
However hard to detect the message would be by the human eye, however, a
computer performing statistical analysis would still be able to detect a
message’s presence by noting that the randomness, or variance, of the LSBs in
the image change sharply just after the first 7 pixels, which is where our
theoretical message ends. (Randomness is much higher in the message data because
it has been encrypted, and encrypted messages have extremely high entropy) Many
sources have thought to fix this by making an offset part of they key needed to
extract the message. The message would then be inserted in a place other than
the beginning of the image, and could only be found and extracted if the offset
was known. However, this approach is still weak to statistical attacks. A
computer would notice that the randomness of the LSBs was much higher in a
single area of the message.
In this implementation, I suggest a solution to this problem: instead of
placing all message bits into the image in order, I first compute a hash
value of the provided key and then use that value to seed a pseudorandom number
generator (PRNG). The generator is then cycled to produce a sequence of numbers
that represent single pixels in the image. When message bits are written, they
are written to those pixels, which are not necessarily in order or adjacent.
This provides an added layer of security: even if analysis determines that there
is a hidden message in the image, no decryption process can begin without first
getting the message bits out of the image in the correct order, which for long
messages is computationally infeasible.
It should be noted that both scrambled and unscrambled writing methods are
present in Stego.jar, though scrambled writing is used by default. The
unscrambled writing methods can be called from StegImage.java.
A key problem in steganography is both storing and determining the length of a
message. This implementation takes the simple approach and simply stores an
unencrypted 3-byte integer at the beginning of the data stream. In an
unscrambled writing process, doing something like this would be very dangerous.
Any user would be able to notice where the levels of randomness were highest and
then simply look at the beginning of the area to find an integer. Scrambling the
pixel order, however, protects against this, because there is no way to tell
which bits represent part of the message length without knowing the key.
