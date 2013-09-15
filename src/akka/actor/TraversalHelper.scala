package akka.actor

import akka.dispatch._
import java.util.concurrent.ExecutorService
import scala.concurrent.forkjoin.ForkJoinPool

object TraversalHelper {

  //case class Settings(samplingPeriod: Int = 5, maxChildren: Int = Int.MaxValue, topActors: Boolean = true)

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
  

  // helpers to access private fields via Java Reflection
  object Reflection {
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
  }
  
  def traverseDispatchers(as: ActorSystem)(handler: DispatcherHandler) = {
    import Reflection._
    
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
}