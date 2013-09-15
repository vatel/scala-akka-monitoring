package akka.actor

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import akka.dispatch._
import java.util.concurrent.ExecutorService
import scala.concurrent.forkjoin.ForkJoinPool
import java.io.{OutputStream, Writer, OutputStreamWriter}
import TraversalHelper._
import scala.collection.mutable.SortedSet
import scala.math.Ordering

object AkkaSampling {

  case class Settings(samplingPeriod: Int = 5,
      maxChildren: Int = Int.MaxValue,
      topSettings: TopSettings = TopSettings())
      
  case class TopSettings(nrOfMsgThreshold: Int = -1, maxActorsToShow: Int = Int.MaxValue)
        
  def print(as: ActorSystem, out: OutputStream = System.out)(implicit settings: Settings = Settings()) = {
    val writer = new OutputStreamWriter(out)

    doAsync(settings.samplingPeriod) {
      doActors(as, writer)
      doDispatchers(as, writer)
      writer.flush
    }
  }
  
  def doActors(as: ActorSystem, writer: Writer)(implicit settings: Settings) = {
    
    implicit val topActorsOrdering = Ordering.fromLessThan[ActorRefWithCell](
        (a1, a2) => a1.underlying.numberOfMessages >= a2.underlying.numberOfMessages)
        
    val topActors = SortedSet()
    def printTop = {
      writer.write("\n=Top actors:=\n")
      topActors.take(settings.topSettings.maxActorsToShow).foreach { actor =>
        writer.write(actor.path + ": " + actor.underlying.numberOfMessages + "\n")
      }
    }

    val ah: ActorHandler = (actor, depth) => {
      val nrOfMessages = actor.underlying.numberOfMessages
      writer.write("  " * depth + actor.path + ": nrOfMessages = " + nrOfMessages + "\n")
      if (nrOfMessages >= settings.topSettings.nrOfMsgThreshold)
        topActors += actor
    }

    writer.write("\n==Actors:==\n")
    traverseActorTree(as)(ah)
    printTop
  }

  def doDispatchers(as: ActorSystem, writer: Writer)(implicit settings: Settings) = {
    val dh: DispatcherHandler = {
      case (dispatcher, Some(pool: ForkJoinPool)) =>
        writer.write(dispatcher.id + ": activeCount = " + pool.getActiveThreadCount +
          ", poolSize = " + pool.getPoolSize + "\n")

      case (dispatcher, Some(execSrv)) => writer.write(dispatcher + " " + execSrv + "\n")

      case (dispatcher, None) => writer.write(dispatcher.toString + "\n")
    }

    writer.write("\n==Dispatchers:==\n")
    traverseDispatchers(as)(dh)
  }
  
  def doAsync(period: Int)(code: => Unit) = {
    new Thread {
      override def run = {
        while (true) {
          code
          Thread.sleep(period * 1000)
        }
      }
    }.start
  }
}