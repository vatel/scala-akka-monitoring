import akka.actor._
import java.io.FileOutputStream

object Test extends App {
  val system = ActorSystem("test")
  
  // this is where we ask to monitor the ActorSystem
  AkkaSampling.print(system)

  // simulate a small actor tree, one of them doing a long "work"
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
}