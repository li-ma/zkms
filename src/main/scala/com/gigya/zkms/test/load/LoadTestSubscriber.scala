package com.gigya.zkms.test.load

import com.gigya.zkms.zkmsService
import com.gigya.zkms.zkmsService._
import org.sellmerfud.optparse._
import com.gigya.zkms.MessageReceived
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Callable
import com.google.common.util.concurrent.AtomicDouble

object LoadTestSubscriber {

  def main(args: Array[String]) {
    
    case class Config(
      zookeeper: String = null,
      topic: String = null
      ) {
      def validate = {
        if (zookeeper.isNullOrEmpty) throw new OptionParserException("--zookeeper not specified");
        if (topic.isNullOrEmpty) throw new OptionParserException("--topic not specified");
        this
      }
    }

    val parser = new OptionParser[Config] {
      banner = "Broadcaster [options] message"
      separator("")
      separator("Options:")
      reqd[String]("-z CONNECTION", "--zookeeper=CONNECTION", "Zookeeper connection string") { (v, c) => c.copy(zookeeper = v) }
      reqd[String]("-t TOPIC", "--topic=TOPIC", "Message topic") { (v, c) => c.copy(topic = v) }
      
    }
    val config = try {
      parser.parse(args, Config()).validate 
    }
    catch {
      case e: OptionParserException => {
        println(e.getMessage + ". Usage:\n" + parser); sys.exit(1)
      }
    }

    val service = new zkmsService(config.zookeeper)
    service.subscribe(config.topic, message)
    var line:String=null;
    while ({line = Console.readLine; line} != null){
      if (line == "quit" || line == "q") {
        pool.shutdown()
        service.unsubscribe(config.topic)
        service.shutdown
        sys.exit(0)
      }
      if (line == "c") {
        val n = msgNum.get()
        System.out.print("\rgot for now: "  + n + "               ");
        msgNum = new AtomicInteger
      }

    } 
  }
  
  var msgNum = new AtomicInteger
  val pool: ExecutorService = Executors.newFixedThreadPool(5)
  def task = new Callable[Unit]() {
        def call(): Unit = {
          val n = msgNum.incrementAndGet()
          System.out.print("\rgot: "  + n + "               ");
        }
      }  
  
  def message(msg:MessageReceived) {
    pool.submit(task)
  }
}



