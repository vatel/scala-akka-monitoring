package akka.actor

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import akka.dispatch._
import java.util.concurrent.ExecutorService
import scala.concurrent.forkjoin.ForkJoinPool

object AkkaMonitoring {

  case class Settings(samplingPeriod: Int = 5, maxChildren: Int = Int.MaxValue, topActors: Boolean = true)

  def jmx(as: ActorSystem)(implicit settings: Settings = Settings()) = {
  }

  def file(as: ActorSystem, file: String)(implicit settings: Settings = Settings()) = {
    //val out = new BufferedOutputStream(new FileOutputStream(file))
    
  }
  
  def print(as: ActorSystem)(implicit settings: Settings = Settings()) = {
    val ah: ActorHandler =
      (actor, depth) => println("  " * depth + actor.path + ": nrOfMessages = " + actor.underlying.numberOfMessages)

    val dh: DispatcherHandler = {
      case (dispatcher, Some(pool: ForkJoinPool)) =>
        println(dispatcher.id + ": activeCount = " + pool.getActiveThreadCount + ", poolSize = " + pool.getPoolSize)
        
      case (dispatcher, Some(execSrv)) => println(dispatcher + " " + execSrv)
      
      case (dispatcher, None) => println(dispatcher)
    }
      
    //startMonitoring(settings, as, ah, dh)
    doAsync(settings.samplingPeriod) {
      println("\n==Actors:==")
      traverseActorTree(as)(ah)
      
      println("\n==Dispatchers:==")
      traverseDispatchers(as)(dh)
    }
  }

  type Depth = Int
  type ActorHandler = (ActorRefWithCell, Depth) => Unit
  type DispatcherHandler = (MessageDispatcher, Option[ExecutorService]) => Unit

  def traverseActorTree(as: ActorSystem)(handler: ActorHandler) = {
    
    def traverseSubTree(node: ActorRefWithCell, depth: Depth): Unit = {
      handler(node, depth)
      node.underlying.childrenRefs.children foreach { actorRef =>
        traverseSubTree(actorRef.asInstanceOf[ActorRefWithCell], depth + 1)
      }
    }
    
    val system = as.asInstanceOf[ActorSystemImpl]
    traverseSubTree(system.guardian, 0)
  }

  val dispConfiguratorsField = {
    val f = classOf[Dispatchers].getDeclaredField("dispatcherConfigurators")
    f.setAccessible(true)
    f
  }
  
  val dispExecSrvGetter = {
    val f = classOf[Dispatcher].getDeclaredMethod("executorService")
    f.setAccessible(true)
    f
  }
  
  def traverseDispatchers(as: ActorSystem)(handler: DispatcherHandler) = {
    
    def traverseDispatcher(dispatcher: MessageDispatcher) = {      

      def extractExecSrv = dispatcher match {
	      // default akka dispatcher
	      case md: Dispatcher => Some(
	        dispExecSrvGetter.invoke(dispatcher)
	          .asInstanceOf[ExecutorServiceDelegate]
	          .executor)
	          
	      case _ => None
      }
      handler(dispatcher, extractExecSrv)
    }
    
    
    
    import scala.collection.JavaConverters._
    
    val confMap = dispConfiguratorsField.get(as.dispatchers)
    	.asInstanceOf[java.util.concurrent.ConcurrentMap[String, MessageDispatcherConfigurator]]
    	.asScala

    confMap.values foreach { dispConf => traverseDispatcher(dispConf.dispatcher) }
  }

  /*def startMonitoring(settings: Settings, as: ActorSystem, ah: ActorHandler, dh: DispatcherHandler) = {
    doAsync(settings.samplingPeriod) {
      traverseActorTree(as)(ah)
      traverseDispatchers(as)(dh)
    }
  }*/
  
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