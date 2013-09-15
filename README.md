scala-akka-monitoring
=====================

Experiments with monitoring of Akka actors


Motivation
==========

Akka, the great actors framework, does not have monitoring out-of-the box.
I have played with TypeSafe Console for some time - really, an admirable stuff, beautiful and handfull for deep data analysis. But it is not applicable for projects of small or medium size we are working now - it is very expensive and, most important, we do not want the monitoring be so deep!
After years of Java I can say that in most cases you need some simple tools for lightweight monitoring - to see how many threads are working, how many messages are in queues etc.
Even better if you have a simple open-source tool which you can adapt to your needs.


How it works
============

If you are familiar with profilers there are 2 concepts: sampling and instrumentation. Typesafe Console does instrumentation - it "patches" some akka classes with AspectJ to intercept some calls and gather info for further analysis.
My tool utilizes sampling - it periodically traverses the whole actor tree and all dispatchers having a reference to ActorSystem. This reference is all that it needs to work.

Access to actor tree is rather simple, excepting that the tool must be inside "akka.actor.package" (otherwise you can not compile it). Going through dispatchers was only possible with Java reflection because of private fields.

I belive such monitoring is better to be added to Akka itself, without hacks and reflection - one day maybe ;)


Examples of usage
=================
val system: ActorSystem = ...

// this will print info about actors and dispatcher to stdout every 5 seconds
AkkaSampling.print(system)

Look into Test.scala


What you can with this tool
===========================
- See the whole actor tree (can be turned on/off soon)
- See "top actors" (with maximum nr of messages in mailbox), some params are tunable
- See all MessageDispatchers (currently only default Dispatcher is supported) with their ExecutorServices (currently details are only shown for ForkJoinPool which is default)


What you can not
================
Time-related measurements - ex. average message procesing/wait time. This is where Typesafe Console shines!


Further ideas
=============
- test on real projects
- add JMX control - start/stop via JMX, change settings via JMX
- publish monitoring data via JMX - not sure whether it is good or bad...
- filter (inclusion/exclusion) for actors
- support for several ActorSystems at once
- play with AspectJ to implement something similar to Typesafe Console but without GUI
