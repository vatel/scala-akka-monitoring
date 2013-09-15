package akka.actor

import akka.dispatch.MessageDispatcherConfigurator
import akka.dispatch.ExecutorServiceDelegate
import scala.concurrent.forkjoin.ForkJoinPool

object Test extends App {
  val system = ActorSystem("test").asInstanceOf[ActorSystemImpl]
  
  val actor1 = system.actorOf(Props(new Actor {
    context.actorOf(Props(new Actor {
      def receive = { case _ => }
    }), "actor11")
    def receive = { case _ => Thread.sleep(10000) }
  }), "actor1")
  
  system.actorOf(Props(new Actor {
    def receive = { case _ => }
  }), "actor2")
  
  (1 to 100) foreach { _ => actor1 ! "Hello" }
  
  val children = system.guardian.underlying.children
  println(children)
  
  val aref = children.head.asInstanceOf[ActorRefWithCell]
  
  val acell = aref.underlying.asInstanceOf[ActorCell]
  val msgNum = acell.mailbox.numberOfMessages
  println("msgNum=" + msgNum)
  
  //acell.mailbox.messageQueue
  
  val subChildren = aref.underlying.childrenRefs.children
  println(subChildren)
  
  val f = system.dispatchers.getClass().getDeclaredField("dispatcherConfigurators")
  f.setAccessible(true)
  val confMap = f.get(system.dispatchers).asInstanceOf[java.util.Map[String, MessageDispatcherConfigurator]]
  val dispatcher = confMap.values.iterator.next.dispatcher
  println(dispatcher)
  
  val ff = dispatcher.getClass
  val f2 = dispatcher.getClass.getDeclaredMethod("executorService")
  f2.setAccessible(true)
  val es = f2.invoke(dispatcher).asInstanceOf[ExecutorServiceDelegate].executor
  println(es)
  
  val fjPool = es.asInstanceOf[ForkJoinPool]
  println("active threads = " + fjPool.getActiveThreadCount)
  println("pool size = " + fjPool.getPoolSize)
}
