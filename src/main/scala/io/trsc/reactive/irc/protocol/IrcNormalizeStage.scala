package io.trsc.reactive.irc.protocol

import akka.stream.stage.{Context, PushStage, SyncDirective}
import akka.util.ByteString

import scala.annotation.tailrec

class IrcNormalizeStage extends PushStage[ByteString, ByteString] {

  private val ColorStart = 3
  private val Zero = 48
  private val Nine = 57
  private val Comma = 44

  private val Plain = 15
  private val Bold = 2
  private val Italics = 29
  private val Underline = 31
  private val Reverse = 22


  def onPush(bytes: ByteString, ctx: Context[ByteString]): SyncDirective = {
    ctx.push(normalize(bytes))
  }

  def normalize(input: ByteString): ByteString = {
    val len = input.length

    def isInt(b: Byte) = b >= Zero && b <= Nine
    def isComma(b: Byte) = b == Comma

    @tailrec
    def findColorEnd(start: Int, foundInts: Int, foundComma: Boolean): Int = {
      if (len > start) {
        val current = input(start)
        if (foundInts < 2 && !foundComma) {
          current match {
            case b if isInt(b) => findColorEnd(start + 1, foundInts + 1, foundComma = false)
            case b if isComma(b) => findColorEnd(start + 1, 0, foundComma = true)
            case _ => start - 1
          }
        } else if (foundInts < 2 && foundComma) {
          current match {
            case b if isInt(b) => findColorEnd(start + 1, foundInts + 1, foundComma = true)
            case _ => start - 1
          }
        } else {
          start - 1
        }
      } else {
        start - 1
      }
    }

    @tailrec
    def loop(pos: Int, acc: ByteString): ByteString = {
      if (len > pos) {
        val idx = input.indexWhere(b =>
          b == ColorStart ||
            b == Plain ||
            b == Bold ||
            b == Italics ||
            b == Underline ||
            b == Reverse
          , pos)
        if (idx != -1) {
          input(idx) match {
            case b if b == Plain || b == Bold || b == Italics || b == Underline || b == Reverse =>
              loop(idx + 1, acc ++ input.slice(pos, idx))
            case ColorStart =>
              val end = findColorEnd(idx + 1, 0, foundComma = false)
              loop(end + 1, acc ++ input.slice(pos, idx))
            case _ => input
          }
        } else if (pos == 0) {
          input
        } else {
          acc ++ input.slice(pos, len)
        }
      } else {
        acc
      }
    }
    loop(0, ByteString.empty)
  }

}
