package com.tenderowls.match3.server.actors

import akka.actor.typed.{ActorRef, Behavior, Terminated}
import akka.actor.typed.scaladsl.Behaviors
import com.tenderowls.match3.BoardOperation.{Swap, Update}
import com.tenderowls.match3.server.data.Score
import com.tenderowls.match3.{Board, BoardAdviser}

import scala.concurrent.duration._

object PlayerActor {

  import BoardAdviser._

  private def suggestSwap(board: Board) = {
    def applySwap(swap: Swap) = {
      def aux(acc: Int, board: Board): Int = {
        board.matchedSequence match {
          case Some(matched) =>
            val ops = board.calculateRemoveSequenceOperations(matched)
            val (removeOps, _) = ops.partition(_.isInstanceOf[Update])
            val boardWithEmpties = board.applyOperations(ops)
            aux(acc + removeOps.length, boardWithEmpties)
          case None =>
            acc
        }
      }
      aux(0, board.applyOperations(List(swap))) -> swap
    }
    board.advices
      .toList
      .map(applySwap)
      .sortBy(-_._1)
      .headOption
      .map(_._2)
  }

  def localPlayer[U](name: String)(onEvent: PartialFunction[Event, U]): Behavior[Event] = {
    Behaviors.receive[Event] {
      case (_, Event.WhatsYourName(replyTo)) =>
        replyTo ! name
        Behaviors.same
      case (_, event) =>
        onEvent(event)
        Behaviors.same
    }
//    Actor.immutable[Event] {
//      case (ctx, msg @ Event.GameStarted(initialBoard, game, _)) =>
//        ctx.watch(game)
//        def ready(board: Board): Behavior[Event] = {
//          Actor.immutable[Event] { (_, msg) =>
//            onEvent(msg)
//            msg match {
//              case Event.EndOfTurn =>
//                Actor.same
//              case Event.MoveResult(batch) =>
//                ready(board.applyOperations(batch.flatten))
//              case Event.WhatsYourName(replyTo) =>
//                replyTo ! name
//                Actor.same
//              case _: Event.YourTurn =>
//                suggestSwap(board).foreach { swap =>
//                  game ! GameActor.Event.MakeMove(ctx.self, swap)
//                }
//                Actor.same
//              case _ => Actor.same
//            }
//          } onSignal {
//            case (_, Terminated(`game`)) =>
//              Actor.stopped
//          }
//        }
//        println("autopray ready")
//        onEvent(msg)
//        ready(initialBoard)
//      case (_, Event.WhatsYourName(replyTo)) =>
//        replyTo ! name
//        Actor.same
//      case _ =>
//        Actor.same
//    }
  }

  def bot(botName: String): Behavior[Event] = {

    Behaviors.receive[Event] {
      case (ctx, Event.GameStarted(initialBoard, game, _)) =>
        ctx.watch(game)
        def ready(board: Board): Behavior[Event] = {
          Behaviors.receive[Event] { (_, msg) =>
            msg match {
              case Event.EndOfTurn =>
                Behaviors.same
              case Event.MoveResult(batch) =>
                val flatBatch = batch.flatten
                // TODO export animation time to config
//                val delay = 0.2.seconds * flatBatch.length
//                ctx.schedule(delay, game, GameActor.Event.AnimationFinished)
                ready(board.applyOperations(flatBatch))
              case Event.WhatsYourName(replyTo) =>
                replyTo ! botName
                Behaviors.same
              case _: Event.YourTurn =>
                suggestSwap(board).foreach { swap =>
                  game ! GameActor.Event.MakeMove(ctx.self, swap)
                }
                Behaviors.same
              case _: Event.OpponentTurn =>
                Behaviors.same
              case _: Event.GameStarted =>
                Behaviors.same
              case Event.YouWin =>
                Behaviors.same
              case Event.YouLose =>
                Behaviors.same
              case _: Event.CurrentScore =>
                Behaviors.same
            }
          } receiveSignal {
            case (_, Terminated(`game`)) =>
              Behaviors.stopped
          }
        }
        ready(initialBoard)
      case (_, Event.WhatsYourName(replyTo)) =>
        replyTo ! botName
        Behaviors.same
      case _ =>
        Behaviors.same
    }
  }

  sealed trait Event

  object Event {
    final case class GameStarted(board: Board, game: Game, opponent: Player) extends Event
    final case class MoveResult(batch: Batch) extends Event
    final case class CurrentScore(your: Score, opponent: Score) extends Event
    final case class YourTurn(time: FiniteDuration) extends Event
    final case class OpponentTurn(time: FiniteDuration) extends Event
    final case class WhatsYourName(replyTo: ActorRef[String]) extends Event

    case object YouWin extends Event
    case object YouLose extends Event
    case object EndOfTurn extends Event
  }
}
