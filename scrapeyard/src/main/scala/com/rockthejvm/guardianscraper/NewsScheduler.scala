package com.rockthejvm.guardianscraper

import org.quartz.*
import org.quartz.impl.StdSchedulerFactory

object NewsScheduler {
  private val myGroup = "newsSchedulerGroup"
  
  def main(args: Array[String]): Unit = {
    val scheduler = StdSchedulerFactory.getDefaultScheduler
    scheduler.start()
    
    val job = JobBuilder
      .newJob(classOf[NewsService])
      .withIdentity("newsService", myGroup)
      .build()
    
    val trigger = TriggerBuilder
      .newTrigger()
      .withIdentity("newsSchedulerTrigger", myGroup)
      .startNow()
      .withSchedule(
        SimpleScheduleBuilder
          .simpleSchedule()
          .withIntervalInMinutes(5)
          .repeatForever()
      )
      .build()
    
    scheduler.scheduleJob(job, trigger)
  }
}
