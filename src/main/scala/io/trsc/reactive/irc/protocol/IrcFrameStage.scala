package io.trsc.reactive.irc.protocol

import akka.stream.stage.{Context, StatefulStage, SyncDirective}
import akka.util.ByteString
import scala.annotation.tailrec

class IrcFrameStage extends StatefulStage[ByteString, ByteString] {
  private val separatorBytes = ByteString("\r\n")
  private val firstSeparatorByte = separatorBytes.head
  private var buffer = ByteString.empty
  private var nextPossibleMatch = 0

  def initial = new State {
    def onPush(chunk: ByteString, ctx: Context[ByteString]): SyncDirective = {
      buffer ++= chunk
      emit(doParse(Vector.empty).iterator, ctx)
    }

    @tailrec
    private def doParse(parsedLinesSoFar: Vector[ByteString]): Vector[ByteString] = {
      val possibleMatchPos = buffer.indexOf(firstSeparatorByte, from = nextPossibleMatch)
      if (possibleMatchPos == -1) {
        nextPossibleMatch = buffer.size
        parsedLinesSoFar
      } else if (possibleMatchPos + separatorBytes.size > buffer.size) {
        nextPossibleMatch = possibleMatchPos
        parsedLinesSoFar
      } else {
        if (buffer.slice(possibleMatchPos, possibleMatchPos + separatorBytes.size) == separatorBytes) {
          val parsedLine = buffer.slice(0, possibleMatchPos)
          buffer = buffer.drop(possibleMatchPos + separatorBytes.size)
          nextPossibleMatch -= possibleMatchPos + separatorBytes.size
          doParse(parsedLinesSoFar :+ parsedLine)
        } else {
          nextPossibleMatch += 1
          doParse(parsedLinesSoFar)
        }
      }
    }
  }
}
