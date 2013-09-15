scala-akka-monitoring
=====================

Experiments with monitoring of Akka actors


= Motivation =
Akka, the great actors framework, does not have monitoring out-of-the box.
I have played with TypeSafe Console for some time - really, an admirable stuff, beautiful and handfull for deep data analysis. But it is not applicable for projects of small or medium size we are working now - it is very expensive and, most important, we do not want the monitoring be so deep!
After years of Java I can say that in most cases you need some simple tools for lightweight monitoring - to see how many threads are working, how many messages are in queues etc.
Even better if you have a simple open-source tool which you can adapt to your needs.

= How it works =

= Examples of usage =

= What you can not with this tool =
Time-related measurements - ex. average message procesing/wait time. This is where Typesafe Console shines!

= Further ideas =
1) JMX control - start/stop via JMX, change settings via JMX
2) publish monitoring data via JMX - not sure whether it is good or bad...
3) filter (inclusion/exclusion) for actors
4) support for several ActorSystems at once
5) play with AspectJ to implement something similar to Typesafe Console but without GUI
